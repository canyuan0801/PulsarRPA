package ai.platon.pulsar.test

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.EmulateEventHandler
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.SQLContexts
import org.slf4j.LoggerFactory
import java.net.URL

open class VerboseCrawler(
    val session: PulsarSession = PulsarContexts.createSession()
): AutoCloseable {
    val logger = LoggerFactory.getLogger(VerboseCrawler::class.java)

    var eventHandler: EmulateEventHandler? = null

    constructor(context: PulsarContext) : this(context.createSession())

    fun open(url: String) {
        open(url, "")
    }

    fun open(url: String, args: String) {
        val options = session.options("-refresh $args")
        load(url, options)
    }

    fun load(url: String, args: String) {
        val options = session.options(args)
        load(url, options)
    }

    fun load(url: String, options: LoadOptions) {
        options.addEventHandler(eventHandler)
        val page = session.load(url, options)
        options.removeEventHandler(eventHandler)
        val doc = session.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        if (options.outLinkSelector.isBlank()) {
            return
        }

        doc.select(options.outLinkSelector) { it.attr("abs:href") }.asSequence()
            .filter { UrlUtils.isValidUrl(it) }
            .mapTo(HashSet()) { it.substringBefore(".com") }
            .asSequence()
            .filter { it.isNotBlank() }
            .mapTo(HashSet()) { "$it.com" }
            .filter { NetUtil.testHttpNetwork(URL(it)) }
            .take(10)
            .joinToString("\n") { it }
            .also { println(it) }

        val path = session.export(doc)
        logger.info("Export to: file://{}", path)
    }

    fun loadOutPages(portalUrl: String, args: String): Collection<WebPage> {
        return loadOutPages(portalUrl, LoadOptions.parse(args, session.sessionConfig))
    }

    fun loadOutPages(portalUrl: String, options: LoadOptions): Collection<WebPage> {
        options.addEventHandler(eventHandler)
        val page = session.load(portalUrl, options)
        options.removeEventHandler(eventHandler)

//        val page = session.load(portalUrl, options)
        if (!page.protocolStatus.isSuccess) {
            logger.warn("Failed to load page | {}", portalUrl)
        }

        val document = session.parse(page)
        document.absoluteLinks()
        document.stripScripts()
        val path = session.export(document)
        logger.info("Portal page is exported to: file://$path")

        val links = document.select(options.correctedOutLinkSelector) { it.attr("abs:href") }
            .mapTo(mutableSetOf()) { session.normalize(it, options) }
            .take(options.topLinks).map { it.spec }
        logger.info("Total {} items to load", links.size)

        val itemOptions = options.createItemOptions(session.sessionConfig).apply { parse = true }
        options.addEventHandler(eventHandler)
        val pages = session.loadAll(links, itemOptions)
        options.removeEventHandler(eventHandler)

        return pages
    }

    override fun close() {
        SQLContexts.shutdown()
    }
}
