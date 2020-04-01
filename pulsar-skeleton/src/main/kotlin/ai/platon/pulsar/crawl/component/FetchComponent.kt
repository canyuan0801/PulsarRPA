/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.FetchMetrics
import ai.platon.pulsar.crawl.protocol.Content
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.ProtocolNotFound
import ai.platon.pulsar.crawl.protocol.ProtocolOutput
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.SharedMetricRegistries
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 17-5-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * Fetch component
 */
@Component
open class FetchComponent(
        val fetchMetrics: FetchMetrics,
        val protocolFactory: ProtocolFactory,
        val immutableConfig: ImmutableConfig
) : AutoCloseable {
    protected val log = LoggerFactory.getLogger(FetchComponent::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }

    private val closed = AtomicBoolean()

    val isClosed get() = closed.get()

    /**
     * Fetch a url
     *
     * @param url The url of web page to fetch
     * @return The fetch result
     */
    fun fetch(url: String): WebPage {
        return fetchContent(WebPage.newWebPage(url, false))
    }

    /**
     * Fetch a url
     *
     * @param url The url of web page to fetch
     * @param options The options
     * @return The fetch result
     */
    fun fetch(url: String, options: LoadOptions): WebPage {
        return fetchContent(createFetchEntry(url, options))
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    fun fetchContent(page: WebPage): WebPage {
        return page.takeIf { isClosed } ?: fetchContent0(page)
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    suspend fun fetchContentDeferred(page: WebPage): WebPage {
        return page.takeIf { isClosed } ?: fetchContentDeferred0(page)
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    protected fun fetchContent0(page: WebPage): WebPage {
        return try {
            fetchMetrics.markTaskStart()
            val protocol = protocolFactory.getProtocol(page)
            processProtocolOutput(page, protocol.getProtocolOutput(page))
        } catch (e: ProtocolNotFound) {
            log.warn("No protocol found | {}", page.url)
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND, CrawlStatus.STATUS_UNFETCHED) }
        }
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    protected suspend fun fetchContentDeferred0(page: WebPage): WebPage {
        return try {
            fetchMetrics.markTaskStart()
            val protocol = protocolFactory.getProtocol(page)
            processProtocolOutput(page, protocol.getProtocolOutputDeferred(page))
        } catch (e: ProtocolNotFound) {
            log.warn(e.message)
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND, CrawlStatus.STATUS_UNFETCHED) }
        }
    }

    protected fun processProtocolOutput(page: WebPage, output: ProtocolOutput): WebPage {
        val url = page.url
        val content = output.content
        if (content == null) {
            log.warn("No content | {}", page.configuredUrl)
        }

        page.headers.putAll(output.headers.asMultimap())
        val protocolStatus = output.protocolStatus

        val crawlStatus = when (protocolStatus.minorCode) {
            ProtocolStatus.SUCCESS_OK -> CrawlStatus.STATUS_FETCHED
            ProtocolStatus.NOTMODIFIED -> CrawlStatus.STATUS_NOTMODIFIED
            ProtocolStatus.CANCELED -> CrawlStatus.STATUS_UNFETCHED

            ProtocolStatus.MOVED,
            ProtocolStatus.TEMP_MOVED -> handleMoved(page, protocolStatus).also { fetchMetrics.trackMoved(url) }

            ProtocolStatus.ACCESS_DENIED,
            ProtocolStatus.ROBOTS_DENIED,
            ProtocolStatus.UNKNOWN_HOST,
            ProtocolStatus.GONE,
            ProtocolStatus.NOTFOUND -> CrawlStatus.STATUS_GONE.also { fetchMetrics.trackHostGone(url) }

            ProtocolStatus.EXCEPTION,
            ProtocolStatus.RETRY,
            ProtocolStatus.BLOCKED -> CrawlStatus.STATUS_RETRY

            ProtocolStatus.REQUEST_TIMEOUT,
            ProtocolStatus.THREAD_TIMEOUT,
            ProtocolStatus.WEB_DRIVER_TIMEOUT,
            ProtocolStatus.SCRIPT_TIMEOUT -> CrawlStatus.STATUS_RETRY.also { fetchMetrics.trackTimeout(url) }

            else -> CrawlStatus.STATUS_RETRY.also { log.warn("Unknown protocol status $protocolStatus") }
        }

        if (crawlStatus.isFetched) {
            fetchMetrics.trackSuccess(page)
        } else if (crawlStatus.isFailed) {
            fetchMetrics.trackFailed(url)
        }

        return when(crawlStatus) {
            CrawlStatus.STATUS_FETCHED,
            CrawlStatus.STATUS_REDIR_TEMP,
            CrawlStatus.STATUS_REDIR_PERM -> updatePage(page, content, protocolStatus, crawlStatus)

            else -> updatePage(page, null, protocolStatus, crawlStatus)
        }
    }

    private fun handleMoved(page: WebPage, protocolStatus: ProtocolStatus): CrawlStatus {
        val crawlStatus: CrawlStatus
        val url = page.url
        val minorCode = protocolStatus.minorCode
        val temp: Boolean
        if (minorCode == ProtocolStatus.MOVED) {
            crawlStatus = CrawlStatus.STATUS_REDIR_PERM
            temp = false
        } else {
            crawlStatus = CrawlStatus.STATUS_REDIR_TEMP
            temp = true
        }

        val newUrl = protocolStatus.getArgOrDefault(ProtocolStatus.ARG_REDIRECT_TO_URL, "")
        if (newUrl.isNotEmpty()) {
            // handleRedirect(url, newUrl, temp, PROTOCOL_REDIR, fetchTask.getPage());
            val reprUrl = URLUtil.chooseRepr(url, newUrl, temp)
            if (reprUrl.length >= AppConstants.SHORTEST_VALID_URL_LENGTH) {
                page.reprUrl = reprUrl
            }
        }
        return crawlStatus
    }

    fun createFetchEntry(originalUrl: String, options: LoadOptions): WebPage {
        return WebPage.newWebPage(originalUrl, options.shortenKey, options.volatileConfig).also {
            it.fetchMode = options.fetchMode
            it.options = options.toString()
        }
    }

    fun initFetchEntry(page: WebPage, options: LoadOptions): WebPage {
        return page.also {
            it.volatileConfig = options.volatileConfig
            it.fetchMode = options.fetchMode
            it.options = options.toString()
        }
    }

    private fun updatePage(page: WebPage, content: Content?,
                           protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus): WebPage {
        updateStatus(page, protocolStatus, crawlStatus)

        if (content != null && protocolStatus.isSuccess) {
            // No need to update content if the fetch is failed, just keep the last content in such cases
            updateContent(page, content)
        }

        updateFetchTime(page)
        updateMarks(page)

        return page
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            fetchMetrics.formatTraffic()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FetchComponent::class.java)

        @JvmStatic
        fun updateStatus(page: WebPage, protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus) {
            page.crawlStatus = crawlStatus
            page.protocolStatus = protocolStatus
            page.increaseFetchCount()
        }

        @JvmStatic
        fun updateMarks(page: WebPage) {
            val marks = page.marks
            marks.putIfNotNull(Mark.FETCH, marks[Mark.GENERATE])
        }

        @JvmStatic
        fun updateContent(page: WebPage, content: Content) {
            updateContent0(page, content, null)
        }

        private fun updateContent0(page: WebPage, content: Content, contentTypeHint: String?) {
            var contentType = contentTypeHint

            page.location = content.baseUrl
            page.setContent(content.content)
            if (contentType != null) {
                content.contentType = contentType
            } else {
                contentType = content.contentType
            }

            if (contentType != null) {
                page.contentType = contentType
            } else {
                log.warn("Failed to determine content type!")
            }
        }

        @JvmStatic
        @JvmOverloads
        fun updateFetchTime(page: WebPage, newFetchTime: Instant = Instant.now()) {
            page.prevFetchTime = page.fetchTime
            page.fetchTime = newFetchTime
            page.putFetchTimeHistory(newFetchTime)
        }
    }
}
