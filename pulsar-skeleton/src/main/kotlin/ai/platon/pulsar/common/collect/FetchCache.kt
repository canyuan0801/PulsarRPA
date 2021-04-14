package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.concurrent.ConcurrentPassiveExpiringSet
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.*
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

interface FetchCache {
    val name: String
    val nonReentrantQueue: Queue<UrlAware>
    val nReentrantQueue: Queue<UrlAware>
    val reentrantQueue: Queue<UrlAware>
    val queues: Array<Queue<UrlAware>>
        get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    val size get() = queues.sumOf { it.size }
    val estimatedSize get() = queues.sumOf { it.size }
}

open class ConcurrentFetchCache(
    override val name: String = "",
    conf: ImmutableConfig
) : FetchCache {
    override val nonReentrantQueue = ConcurrentNonReentrantQueue<UrlAware>()
    override val nReentrantQueue = ConcurrentNEntrantQueue<UrlAware>(3)
    override val reentrantQueue = ConcurrentLinkedQueue<UrlAware>()
}

class LoadingFetchCache(
    override val name: String = "",
    val urlLoader: ExternalUrlLoader,
    val priority: Int = Priority13.NORMAL.value,
    val capacity: Int = LoadingQueue.DEFAULT_CAPACITY
) : FetchCache, Loadable<UrlAware> {

    companion object {
        const val G_NON_REENTRANT = 1
        const val G_N_ENTRANT = 2
        const val G_REENTRANT = 3
    }

    override val nonReentrantQueue = ConcurrentNonReentrantLoadingQueue(urlLoader, G_NON_REENTRANT, priority, capacity)
    override val nReentrantQueue = ConcurrentNEntrantLoadingQueue(urlLoader, 3, G_N_ENTRANT, priority, capacity)
    override val reentrantQueue = ConcurrentLoadingQueue(urlLoader, G_REENTRANT, priority, capacity)
    override val queues: Array<Queue<UrlAware>>
        get() = arrayOf(nonReentrantQueue, nReentrantQueue, reentrantQueue)
    override val size get() = queues.sumOf { it.size }
    override val estimatedSize: Int
        get() = queues.filterIsInstance<LoadingQueue<UrlAware>>().sumOf { it.size + it.estimatedExternalSize }

    override fun load() {
        queues.filterIsInstance<Loadable<UrlAware>>().forEach { it.load() }
    }

    override fun loadNow(): Collection<UrlAware> {
        return queues.filterIsInstance<Loadable<UrlAware>>().flatMap { it.loadNow() }
    }
}
