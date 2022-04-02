package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.collect.collector.PriorityDataCollector
import ai.platon.pulsar.common.collect.collector.QueueCollector
import ai.platon.pulsar.common.collect.collector.UrlCacheCollector
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CollectorHelper(val feeder: UrlFeeder) {
    private val dcLogger = getLogger(DataCollector::class)
    private val fetchCaches get() = feeder.fetchCaches

    fun getCollectors(name: String): List<PriorityDataCollector<UrlAware>> = feeder.getCollectors(name)

    fun getCollectors(names: Iterable<String>): List<PriorityDataCollector<UrlAware>> =
        feeder.getCollectors(names)

    fun getCollectors(regex: Regex): List<PriorityDataCollector<UrlAware>> = feeder.getCollectors(regex)

    fun getCollectorsLike(name: String): List<PriorityDataCollector<UrlAware>> = feeder.getCollectorsLike(name)

    fun contains(name: String): Boolean = getCollectors(name).isNotEmpty()

    fun contains(names: Iterable<String>): Boolean = getCollectors(names).isNotEmpty()

    fun contains(regex: Regex): Boolean = getCollectors(regex).isNotEmpty()

    fun containsLike(name: String): Boolean = getCollectorsLike(name).isNotEmpty()

    fun addDefaults() {
        feeder.addDefaultCollectors()
    }

    fun add(collector: PriorityDataCollector<UrlAware>) {
        addAll(listOf(collector))
    }

    fun addAll(collectors: Iterable<PriorityDataCollector<UrlAware>>) {
        collectors.filterIsInstance<UrlCacheCollector>().forEach {
            fetchCaches.unorderedCaches.add(it.urlCache)
        }
        collectors.forEach { report(it) }
        feeder.addCollectors(collectors)
    }

    fun addFetchCacheCollector(priority: Int, urlLoader: ExternalUrlLoader): UrlCacheCollector {
        return addFetchCacheCollector("", priority, urlLoader).also { it.name = "LFC@" + it.id }
    }

    fun addFetchCacheCollector(name: String, priority: Int, urlLoader: ExternalUrlLoader): UrlCacheCollector {
        val fetchCache = LoadingUrlCache(name, priority, urlLoader)
        fetchCaches.unorderedCaches.add(fetchCache)
        val collector = UrlCacheCollector(fetchCache).also { it.name = name }

        report(collector)
        feeder.addCollector(collector)

        return collector
    }

    fun addFetchCacheCollector(priority: Int): UrlCacheCollector {
        return addFetchCacheCollector("", priority).also { it.name = "FC@" + it.id }
    }

    fun addFetchCacheCollector(name: String, priority: Int): UrlCacheCollector {
        val fetchCache = ConcurrentUrlCache(name)
        fetchCaches.unorderedCaches.add(fetchCache)
        val collector = UrlCacheCollector(fetchCache).also { it.name = name }

        feeder.addCollector(collector)
        report(collector)

        return collector
    }

    fun addQueueCollector(
        name: String,
        priority: Int = Priority13.NORMAL.value,
        queue: Queue<UrlAware> = ConcurrentLinkedQueue()
    ): QueueCollector {
        val collector = QueueCollector(queue, priority).also { it.name = name }

        feeder.addCollector(collector)
        report(collector)

        return collector
    }

    fun remove(name: String): DataCollector<UrlAware>? {
        return removeAll(listOf(name)).firstOrNull()
    }

    fun removeAll(names: Iterable<String>): Collection<DataCollector<UrlAware>> {
        val collectors = feeder.getCollectors(names)
        return removeAll(collectors)
    }

    fun removeAll(regex: Regex): Collection<DataCollector<UrlAware>> {
        val collectors = feeder.getCollectors(regex)
        return removeAll(collectors)
    }

    fun removeAllLike(name: String): Collection<DataCollector<UrlAware>> {
        return removeAll(".*$name.*".toRegex())
    }

    fun removeAll(collectors: Collection<DataCollector<UrlAware>>): Collection<DataCollector<UrlAware>> {
        feeder.removeAll(collectors)
        collectors.filterIsInstance<UrlCacheCollector>().map { it.urlCache }
            .let { fetchCaches.unorderedCaches.removeAll(it) }

        if (collectors.isNotEmpty()) {
            dcLogger.info("Removed collectors: " + collectors.joinToString { it.name })
            collectors.forEachIndexed { i, c -> dcLogger.info("${i + 1}.\t$c") }
            dcLogger.info("")
        }
        return collectors
    }

    fun report(collector: DataCollector<out UrlAware>, message: String = "") {
        val msg = if (message.isBlank()) "" else " | $message"

        dcLogger.info("Task <{}> has {}/{} items{}, adding to {}@{}",
            collector.name, collector.size, collector.estimatedSize, msg,
            feeder.openCollectors.javaClass.simpleName,
            feeder.openCollectors.hashCode())
        dcLogger.info("{}", collector)
    }
}
