package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.AppMetrics
import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.url.Hyperlink
import ai.platon.pulsar.common.url.Hyperlinks
import ai.platon.pulsar.common.url.UrlAware
import com.codahale.metrics.Gauge
import com.google.common.collect.Iterators
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

open class LocalFileHyperlinkCollector(
        val path: Path,
        priority: Int = Priority13.NORMAL.value,
        capacity: Int = DEFAULT_CAPACITY
): AbstractPriorityDataCollector<Hyperlink>(priority, capacity) {

    private val log = LoggerFactory.getLogger(LocalFileHyperlinkCollector::class.java)

    private val urlLoader = LocalFileUrlLoader(path)
    val fileName = path.fileName.toString()
    override var name = fileName.substringBefore(".")

    var loadArgs: String? = null
    val hyperlinks = LinkedList<Hyperlink>()

    init {
        val remainingCapacity = capacity - hyperlinks.size
        urlLoader.loadToNow(hyperlinks, remainingCapacity, 0, priority) {
            // TODO: merge args
            Hyperlinks.toHyperlink(it).also { it.args = loadArgs }
        }
        log.info("There are {} urls in file | {}", hyperlinks.size, path)
    }

    constructor(path: Path, priority: Priority13, capacity: Int = DEFAULT_CAPACITY): this(path, priority.value, capacity)

    override fun hasMore() = hyperlinks.isNotEmpty()

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        if (!hasMore()) {
            return 0
        }

        var collected = 0
        hyperlinks.poll()?.let {
            if (sink.add(it)) {
               ++collected
            }
        }

        return collected
    }
}

open class CircularLocalFileHyperlinkCollector(
        path: Path,
        priority: Priority13 = Priority13.NORMAL,
        capacity: Int = DEFAULT_CAPACITY
): LocalFileHyperlinkCollector(path, priority, capacity) {

    protected val iterator = Iterators.cycle(hyperlinks)

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        var collected = 0
        if (hasMore() && iterator.hasNext()) {
            collected += collectTo(iterator.next(), sink)
        }
        return collected
    }
}

open class PeriodicalLocalFileHyperlinkCollector(
        path: Path,
        val options: LoadOptions,
        priority: Priority13 = Priority13.NORMAL,
        capacity: Int = DEFAULT_CAPACITY
): CircularLocalFileHyperlinkCollector(path, priority, capacity) {
    private val log = LoggerFactory.getLogger(PeriodicalLocalFileHyperlinkCollector::class.java)

    companion object {

        data class Counters(
                var collects: Int = 0,
                var collected: Int = 0,
                var round: Int = 0
        )

        val globalCounters = Counters()

        private val gauges = mapOf(
                "collects" to Gauge { globalCounters.collects },
                "collected" to Gauge { globalCounters.collected },
                "round" to Gauge { globalCounters.round }
        )

        init {
            AppMetrics.registerAll(this, gauges)
        }
    }

    private val position = AtomicInteger()
    val uuid = UUID.randomUUID()
    val counters = Counters()

    var batchSize = 10

    val startTimes = mutableMapOf<Int, Instant>()
    val finishTimes = mutableMapOf<Int, Instant>()

    val round get() = counters.round
    var roundCollected = 0
        private set
    val startTime get() = startTimes[counters.round]?: Instant.EPOCH
    val finishTime get() = finishTimes[counters.round]?: Instant.EPOCH
    val expires get() = options.expires
    val isStarted get() = counters.round > 0
    val isExpired get() = isFinished && (startTime + expires < Instant.now())
    val isFinished get() = position.get() >= hyperlinks.size

    override fun hasMore() = (!isFinished || isExpired) && iterator.hasNext()

    override fun collectTo(sink: MutableList<Hyperlink>): Int {
        ++counters.collects

        resetIfNecessary()

        var i = 0
        var collected = 0
        while (i++ < batchSize && hasMore() && iterator.hasNext()) {
            collected += collectTo(iterator.next(), sink)
            ++counters.collected
            position.incrementAndGet()
        }

        if (isFinished) {
            finishTimes[round] = Instant.now()
        }

        roundCollected += collected
        return collected
    }

    override fun toString(): String {
        return "$name - round: $round collected: ${counters.collected} " +
                "startTime: $startTime expires: $expires priority: $priority"
    }

    private fun resetIfNecessary() {
        if (isExpired) {
            position.set(0)
        }

        if (position.get() == 0) {
            ++counters.round
            roundCollected = 0
            startTimes[round] = Instant.now()

            log.info("Round {} fetching {} hyperlinks in local file | {} {} | {}",
                    round, hyperlinks.size,
                    startTimes[round], expires,
                    path)
        }
    }
}
