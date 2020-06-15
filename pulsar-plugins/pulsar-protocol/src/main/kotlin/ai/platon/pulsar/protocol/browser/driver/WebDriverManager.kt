package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.common.MetricsManagement
import ai.platon.pulsar.common.PreemptChannelSupport
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.proxy.ProxyPoolMonitor
import ai.platon.pulsar.crawl.BrowserInstanceId
import com.codahale.metrics.Gauge
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean

class WebDriverTask<R> (
        val browserId: BrowserInstanceId,
        val priority: Int,
        val volatileConfig: VolatileConfig,
        val action: suspend (driver: ManagedWebDriver) -> R
)

class PoolRetiredException(message: String): IllegalStateException(message)

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebDriverManager(
        val driverControl: WebDriverControl,
        val proxyMonitor: ProxyPoolMonitor,
        val driverFactory: WebDriverFactory,
        val immutableConfig: ImmutableConfig
): Parameterized, PreemptChannelSupport("WebDriverManager"), AutoCloseable {
    private val log = LoggerFactory.getLogger(WebDriverManager::class.java)

    private val taskTimeout = Duration.ofMinutes(5)
    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()
    val startTime = Instant.now()
    val numReset = MetricsManagement.meter(this, "numReset")
    val elapsedTime get() = Duration.between(startTime, Instant.now())
    val driverPools = ConcurrentSkipListMap<BrowserInstanceId, LoadingWebDriverPool>()
    val retiredPools = ConcurrentSkipListSet<BrowserInstanceId>()

    init {
        mapOf(
                "waitingDrivers" to Gauge<Int> { driverPools.values.sumBy { it.numWaiting.get() } },
                "workingDrivers" to Gauge<Int> { driverPools.values.sumBy { it.numWorking.get() } }
        ).forEach { MetricsManagement.register(this, it.key, it.value) }
    }

    /**
     * reactor: tell me if you can do this job
     * proactor: here is a job, tell me if you finished it
     * */
    @Throws(IllegalStateException::class)
    suspend fun <R> run(browserId: BrowserInstanceId, priority: Int, volatileConfig: VolatileConfig,
                        action: suspend (driver: ManagedWebDriver) -> R
    ) = run(WebDriverTask(browserId, priority, volatileConfig, action))

    @Throws(IllegalStateException::class)
    suspend fun <R> run(task: WebDriverTask<R>): R {
        val browserId = task.browserId
        return whenNormalDeferred {
            checkState()

            if (isRetiredPool(browserId)) {
                throw PoolRetiredException("${browserId}")
            }

            val driverPool = driverPools.computeIfAbsent(browserId) { path ->
                require("browser" in path.toString())
                LoadingWebDriverPool(browserId, task.priority, driverFactory, immutableConfig).also {
                    it.allocate(task.volatileConfig)
                }
            }

            val driver = driverPool.take(task.priority, task.volatileConfig).apply { startWork() }
            try {
                supervisorScope {
                    withTimeout(taskTimeout.toMillis()) { task.action(driver) }
                }
            } finally {
                driverPool.put(driver)
            }
        }
    }

    fun isRetiredPool(browserId: BrowserInstanceId) = retiredPools.contains(browserId)

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(url: String): ManagedWebDriver? {
        checkState()
        var driver: ManagedWebDriver? = null
        driverPools.values.forEach { it ->
            driver = it.firstOrNull { it.url == url }?.also { it.cancel() }
        }
        return driver
    }

    /**
     * Cancel the fetch task specified by [url] remotely
     * NOTE: A cancel request should run immediately not waiting for any browser task return
     * */
    fun cancel(browserId: BrowserInstanceId, url: String): ManagedWebDriver? {
        checkState()
        val driverPool = driverPools[browserId] ?: return null
        return driverPool.firstOrNull { it.url == url }?.also { it.cancel() }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll() {
        checkState()
        driverPools.values.forEach { driverPool ->
            driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
        }
    }

    /**
     * Cancel all the fetch tasks, stop loading all pages
     * */
    fun cancelAll(browserId: BrowserInstanceId) {
        checkState()
        val driverPool = driverPools[browserId] ?: return
        driverPool.onlineDrivers.toList().parallelStream().forEach { it.cancel() }
    }

    /**
     * Cancel all running tasks and close all web drivers
     * */
    fun closeDriverPool(browserId: BrowserInstanceId, timeToWait: Duration) {
        checkState()
        numReset.mark()
        // Mark all drivers are canceled
        doCloseDriverPool(browserId)
    }

    fun formatStatus(browserId: BrowserInstanceId, verbose: Boolean = false): String {
        return driverPools[browserId]?.formatStatus(verbose)?:""
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            driverPools.clear()
            log.info("Web driver manager is closed\n{}", toString())
        }
    }

    override fun toString(): String = formatStatus(false)

    private fun doCloseDriverPool(browserId: BrowserInstanceId) {
        checkState()
        preempt {
            retiredPools.add(browserId)
            driverPools.remove(browserId)?.also { driverPool ->
                log.info("Closing driver pool | {} | {}", driverPool.formatStatus(verbose = true), browserId)
                driverPool.close()
            }
        }
    }

    private fun checkState() {
        if (!isActive) throw IllegalStateException("Web driver manager is closed")
    }

    private fun formatStatus(verbose: Boolean = false): String {
        return driverPools.entries.joinToString("\n") { it.value.formatStatus(verbose) + " | " + it.key }
    }
}
