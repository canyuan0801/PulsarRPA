package ai.platon.pulsar.common.proxy

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_POOL_MONITOR_CLASS
import ai.platon.pulsar.common.config.ImmutableConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class ProxyPoolMonitorFactory(
        val proxyPool: ProxyPool,
        val conf: ImmutableConfig
): AutoCloseable {
    private val log = LoggerFactory.getLogger(ProxyPoolMonitorFactory::class.java)

    private val proxyPoolMonitorRef = AtomicReference<ProxyPoolMonitor>()
    fun get(): ProxyPoolMonitor = createIfAbsent(conf)

    override fun close() {
        proxyPoolMonitorRef.getAndSet(null)?.close()
    }

    private fun createIfAbsent(conf: ImmutableConfig): ProxyPoolMonitor {
        if (proxyPoolMonitorRef.get() == null) {
            synchronized(ProxyPoolMonitorFactory::class) {
                if (proxyPoolMonitorRef.get() == null) {
                    val defaultClazz = ProxyPoolMonitor::class.java
                    val clazz = try {
                        conf.getClass(PROXY_POOL_MONITOR_CLASS, defaultClazz)
                    } catch (e: Exception) {
                        log.warn("Configured proxy pool monitor {}({}) is not found, use default ({})",
                                PROXY_POOL_MONITOR_CLASS, conf.get(PROXY_POOL_MONITOR_CLASS), defaultClazz.name)
                        defaultClazz
                    }
                    val ref = clazz.constructors.first { it.parameters.size == 2 }.newInstance(proxyPool, conf)
                    proxyPoolMonitorRef.set(ref as? ProxyPoolMonitor)
                }
            }
        }

        return proxyPoolMonitorRef.get()
    }
}