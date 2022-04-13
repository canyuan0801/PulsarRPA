package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.queue.*
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

interface UrlCache {
    val name: String
    /**
     * The priority
     * */
    val priority: Int
    val nonReentrantQueue: Queue<UrlAware>
    val nReentrantQueue: Queue<UrlAware>
    val reentrantQueue: Queue<UrlAware>
    val queues: List<Queue<UrlAware>>
        get() = listOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    val size get() = queues.sumOf { it.size }
    val externalSize: Int get() = 0
    val estimatedExternalSize get() = 0
    val estimatedSize get() = size + estimatedExternalSize

    fun removeDeceased()
    fun clear()
    fun deepClear() = clear()
}

abstract class AbstractUrlCache(
    override val name: String,
    override val priority: Int
) : UrlCache {
    override fun removeDeceased() {
        val now = Instant.now()
        queues.forEach { it.removeIf { it.deadTime < now } }
    }

    override fun clear() {
        queues.forEach { it.clear() }
    }
}

open class ConcurrentUrlCache(
    name: String = "",
    priority: Int = Priority13.NORMAL.value
) : AbstractUrlCache(name, priority) {
    override val nonReentrantQueue = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantQueue = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantQueue = ConcurrentLinkedQueue<UrlAware>()
}

class LoadingUrlCache constructor(
    /**
     * The cache name, a loading fetch cache requires a unique name
     * */
    name: String,
    /**
     * The priority
     * */
    priority: Int,
    /**
     * The external url loader
     * */
    val urlLoader: ExternalUrlLoader,
    /**
     * The cache capacity
     * */
    val capacity: Int = LoadingQueue.DEFAULT_CAPACITY,
) : AbstractUrlCache(name, priority), Loadable<UrlAware> {

    companion object {
        const val G_NON_REENTRANT = 1
        const val G_N_ENTRANT = 2
        const val G_REENTRANT = 3
    }

    override val nonReentrantQueue = ConcurrentNonReentrantLoadingQueue(urlLoader, topic(G_NON_REENTRANT))
    override val nReentrantQueue = ConcurrentNEntrantLoadingQueue(urlLoader, topic(G_N_ENTRANT), 3)
    override val reentrantQueue = ConcurrentLoadingQueue(urlLoader, topic(G_REENTRANT))
    override val queues: List<Queue<UrlAware>> get() = listOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    override val externalSize: Int
        get() = queues.filterIsInstance<LoadingQueue<UrlAware>>().sumOf { it.externalSize }
    override val estimatedExternalSize: Int
        get() = queues.filterIsInstance<LoadingQueue<UrlAware>>().sumOf { it.estimatedExternalSize }

    override fun load() {
        queues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load() }
    }

    override fun loadNow(): Collection<UrlAware> {
        return queues.filterIsInstance<Loadable<UrlAware>>().flatMap { it.loadNow() }
    }

    override fun deepClear() {
        queues.filterIsInstance<LoadingQueue<UrlAware>>().forEach { it.deepClear() }
    }

    private fun topic(group: Int) = UrlTopic(name, group, priority, capacity)
}