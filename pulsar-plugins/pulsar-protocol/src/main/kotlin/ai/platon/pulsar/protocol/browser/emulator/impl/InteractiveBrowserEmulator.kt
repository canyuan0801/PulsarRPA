package ai.platon.pulsar.protocol.browser.emulator.impl

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.metrics.AppMetrics
import ai.platon.pulsar.common.persist.ext.browseEvent
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.*
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.crawl.protocol.http.ProtocolStatusTranslator
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import ai.platon.pulsar.protocol.browser.driver.SessionLostException
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.emulator.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 */
open class InteractiveBrowserEmulator(
    val driverPoolManager: WebDriverPoolManager,
    responseHandler: BrowserResponseHandler,
    immutableConfig: ImmutableConfig
): BrowserEmulator,
    BrowserEmulatorImplBase(driverPoolManager.driverSettings, responseHandler, immutableConfig)
{
    private val logger = LoggerFactory.getLogger(BrowserEmulator::class.java)!!
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    private val taskLogger = LoggerFactory.getLogger(BrowserEmulator::class.java.name + ".Task")!!
    val numDeferredNavigates by lazy { AppMetrics.reg.meter(this, "deferredNavigates") }

    init {
        attach()
    }

    /**
     * Fetch a page using a browser which can render the DOM and execute scripts.
     *
     * @param task The task to fetch
     * @return The result of this fetch
     * */
    override suspend fun fetch(task: FetchTask, driver: WebDriver): FetchResult {
        return takeIf { isActive }?.browseWithDriver(task, driver) ?: FetchResult.canceled(task)
    }

    override fun cancelNow(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverPoolManager.cancel(task.url)
    }

    override suspend fun cancel(task: FetchTask) {
        counterCancels.inc()
        task.cancel()
        driverPoolManager.cancel(task.url)
    }

    private fun attach() {
        on1(EmulateEvents.willNavigate) { page: WebPage, driver: WebDriver ->
            this.onWillNavigate(page, driver)
        }
        on1(EmulateEvents.navigated) { page: WebPage, driver: WebDriver ->
            this.onNavigated(page, driver)
        }
        on1(EmulateEvents.willCheckDocumentState) { page: WebPage, driver: WebDriver ->
            this.onWillCheckDocumentState(page, driver)
        }
        on1(EmulateEvents.documentActuallyReady) { page: WebPage, driver: WebDriver ->
            this.onDocumentActuallyReady(page, driver)
        }
        on1(EmulateEvents.willComputeFeature) { page: WebPage, driver: WebDriver ->
            this.onWillComputeFeature(page, driver)
        }
        on1(EmulateEvents.featureComputed) { page: WebPage, driver: WebDriver ->
            this.onFeatureComputed(page, driver)
        }
        on1(EmulateEvents.willStopTab) { page: WebPage, driver: WebDriver ->
            this.onWillStopTab(page, driver)
        }
        on1(EmulateEvents.tabStopped) { page: WebPage, driver: WebDriver ->
            this.onTabStopped(page, driver)
        }
    }

    private fun detach() {
        EmulateEvents.values().forEach { off(it) }
    }

    override suspend fun onWillNavigate(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onWillNavigate?.invoke(page, driver)
    }

    override suspend fun onNavigated(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onNavigated?.invoke(page, driver)
    }

    override suspend fun onWillCheckDocumentState(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onWillCheckDocumentState?.invoke(page, driver)
    }

    override suspend fun onDocumentActuallyReady(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onDocumentActuallyReady?.invoke(page, driver)
    }

    override suspend fun onWillComputeFeature(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onWillComputeFeature?.invoke(page, driver)
    }

    override suspend fun onFeatureComputed(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onFeatureComputed?.invoke(page, driver)
    }

    override suspend fun onWillStopTab(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onWillStopTab?.invoke(page, driver)
    }

    override suspend fun onTabStopped(page: WebPage, driver: WebDriver) {
        page.browseEvent?.onTabStopped?.invoke(page, driver)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            detach()
        }
    }

    protected open suspend fun browseWithDriver(task: FetchTask, driver: WebDriver): FetchResult {
        // page.lastBrowser is used by AppFiles.export, so it has to be set before export
        task.page.lastBrowser = driver.browserType

        if (task.page.options.isDead()) {
            taskLogger.info("Page is dead, cancel the task | {}", task.page.configuredUrl)
            return FetchResult.canceled(task)
        }

        var exception: Exception? = null
        var response: Response?

        try {
            checkState(task, driver)

            response = if (task.page.isResource) {
                loadResourceWithoutRendering(task, driver)
            } else browseWithCancellationHandled(task, driver)
        }  catch (e: NavigateTaskCancellationException) {
            // The task is canceled
            response = ForwardingResponse.canceled(task.page)
        } catch (e: WebDriverCancellationException) {
            // The web driver is canceled
            response = ForwardingResponse.canceled(task.page)
        } catch (e: SessionLostException) {
            logger.warn("Web driver session #{} is lost | {}", e.driver?.id, e.brief())
            driver.retire()
            exception = e
            response = ForwardingResponse.privacyRetry(task.page)
        } catch (e: WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                logger.warn("Web driver is disconnected - {}", e.brief())
            } else {
                logger.warn("[Unexpected]", e)
            }

            driver.retire()
            exception = e
            response = ForwardingResponse.crawlRetry(task.page)
        } catch (e: TimeoutCancellationException) {
            logger.warn("[Timeout] Coroutine was cancelled, thrown by [withTimeout] | {}", e.brief())
            response = ForwardingResponse.crawlRetry(task.page, e)
        } catch (e: Exception) {
            when {
                e.javaClass.name == "kotlinx.coroutines.JobCancellationException" -> {
                    logger.warn("Coroutine was cancelled | {}", e.message)
                }
                else -> {
                    logger.warn("[Unexpected]", e)
                }
            }
            response = ForwardingResponse.crawlRetry(task.page, e)
        } finally {
        }

        return FetchResult(task, response ?: ForwardingResponse(exception, task.page), exception)
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    private suspend fun loadResourceWithoutRendering(task: FetchTask, driver: WebDriver): Response {
        checkState(task, driver)

        val navigateTask = NavigateTask(task, driver, driverSettings)

        val response = driver.loadResource(task.url)
            ?: return ForwardingResponse.failed(task.page, SessionLostException("null response"))

        // TODO: transform in AbstractHttpProtocol
        val protocolStatus = ProtocolStatusTranslator.translateHttpCode(response.statusCode())
        navigateTask.pageSource = response.body()
        navigateTask.pageDatum.also {
            it.protocolStatus = protocolStatus
            it.headers.putAll(response.headers())
            it.contentType = response.contentType()
            it.content = navigateTask.pageSource.toByteArray(StandardCharsets.UTF_8)
        }

        responseHandler.emit(BrowserResponseEvents.willCreateResponse)
        return createResponseWithDatum(navigateTask, navigateTask.pageDatum).also {
            responseHandler.emit(BrowserResponseEvents.responseCreated)
        }
    }

    private suspend fun browseWithCancellationHandled(task: FetchTask, driver: WebDriver): Response? {
        checkState(task, driver)

        var response: Response?
        val page = task.page

        try {
            response = browseWithWebDriver(task, driver)

            // Do something like a human being
//            interactAfterFetch(task, driver)

            emit1(EmulateEvents.willStopTab, page, driver)
//            listeners.notify(EventType.willStopTab, page, driver)
//            val event = page.browseEvent
//            notify("onWillStopTab") { event?.onWillStopTab?.invoke(page, driver) }

            /**
             * Force the page stop all navigations and releases all resources.
             * If a web driver is terminated, it should not be used any more and should be quit
             * as soon as possible.
             * */
            driver.stop()

            emit1(EmulateEvents.tabStopped, page, driver)
//            notify("onTabStopped") { event?.onTabStopped?.invoke(page, driver) }
        } catch (e: NavigateTaskCancellationException) {
            logger.info("{}. Try canceled task {}/{} again later (privacy scope suggested)",
                page.id, task.id, task.batchId)
            response = ForwardingResponse.canceled(page)
        }

        return response
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    private suspend fun browseWithWebDriver(task: FetchTask, driver: WebDriver): Response {
        checkState(task, driver)

        val navigateTask = NavigateTask(task, driver, driverSettings)

        val interactResult = navigateAndInteract(task, driver, navigateTask.driverSettings)
        navigateTask.pageDatum.apply {
            protocolStatus = interactResult.protocolStatus
            activeDOMStatTrace = interactResult.activeDOMMessage?.trace
            activeDOMUrls = interactResult.activeDOMMessage?.urls
        }
        navigateTask.pageSource = driver.pageSource() ?: ""

        responseHandler.onWillCreateResponse(task, driver)
        return createResponse(navigateTask).also {
            responseHandler.onResponseCreated(task, driver, it)
        }
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverException::class)
    private suspend fun navigateAndInteract(task: FetchTask, driver: WebDriver, settings: BrowserSettings): InteractResult {
        checkState(task, driver)

        val page = task.page

        logBeforeNavigate(task, settings)
        driver.setTimeouts(settings)
        // TODO: handle frames
        // driver.switchTo().frame(1);

        meterNavigates.mark()
        numDeferredNavigates.mark()

        tracer?.trace("{}. Navigating | {}", page.id, task.url)

        checkState(task, driver)

//        listeners.notify(EventType.willNavigate, page, driver)
//        val event = page.browseEvent
//        notify("onWillNavigate") { event?.onWillNavigate?.invoke(page, driver) }

        // href has the higher priority to locate a resource
        require(task.url == page.url)
        val finalUrl = task.href ?: task.url
        val navigateEntry = NavigateEntry(finalUrl, page.id, task.url, pageReferrer = page.referrer)

        emit1(EmulateEvents.willNavigate, page, driver)

        checkState(task, driver)
        try {
            driver.navigateTo(navigateEntry)
        } finally {
            emit1(EmulateEvents.navigated, page, driver)
//            notify("onNavigated") { event?.onNavigated?.invoke(page, driver) }
        }

        if (!driver.supportJavascript) {
            return InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
        }

        val interactTask = InteractTask(task, settings, driver)
        return if (settings.isStartupScriptEnabled) {
            emit1(EmulateEvents.willInteract, page, driver)
//            notify("onWillInteract") { event?.onWillInteract?.invoke(page, driver) }

            interact(interactTask).also {
                emit1(EmulateEvents.didInteract, page, driver)
//                notify("onDidInteract") { event?.onDidInteract?.invoke(page, driver) }
            }
        } else {
            interactNoJsInvaded(interactTask)
        }
    }

    protected open suspend fun interactNoJsInvaded(interactTask: InteractTask): InteractResult {
        var pageSource = ""
        var i = 0
        do {
            pageSource = interactTask.driver.pageSource() ?: ""
            if (pageSource.length < 20_000) {
                delay(1000)
            }
        } while (i++ < 45 && pageSource.length < 20_000 && isActive)

        return InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
    }

    @Throws(NavigateTaskCancellationException::class, WebDriverCancellationException::class)
    protected open suspend fun interact(task: InteractTask): InteractResult {
        checkState(task.fetchTask, task.driver)

        val result = InteractResult(ProtocolStatus.STATUS_SUCCESS, null)
        val page = task.fetchTask.page
        val driver = task.driver

        tracer?.trace("{}", task.interactSettings)

        emit1(EmulateEvents.willCheckDocumentState, page, driver)

        waitForDocumentActuallyReady(task, result)

        if (result.protocolStatus.isSuccess) {
            task.driver.navigateEntry.documentReadyTime = Instant.now()
            emit1(EmulateEvents.documentActuallyReady, page, driver)
        }

        if (result.state.isContinue) {
            scrollDown(task, result)
        }

        if (result.state.isContinue) {
            emit1(EmulateEvents.willComputeFeature, page, driver)

            computeDocumentFeatures(task, result)

            emit1(EmulateEvents.featureComputed, page, driver)
        }

        return result
    }

    /**
     * Wait until the document is actually ready, or timeout.
     * */
    @Throws(NavigateTaskCancellationException::class)
    protected open suspend fun waitForDocumentActuallyReady(interactTask: InteractTask, result: InteractResult) {
        var status = ProtocolStatus.STATUS_SUCCESS
        val scriptTimeout = interactTask.interactSettings.scriptTimeout
        val fetchTask = interactTask.fetchTask

        val initialScroll = 5
        val delayMillis = 500L * 2
//        val maxRound = scriptTimeout.toMillis() / delayMillis
        val maxRound = 60

        // TODO: wait for expected data, ni, na, nn, nst, etc; required element
        val expression = String.format("__pulsar_utils__.waitForReady(%d)", initialScroll)
        var i = 0
        var message: Any? = null
        try {
            var msg: Any? = null
            // TODO: driver.isWorking
            while ((msg == null || msg == false) && i++ < maxRound && isActive && !fetchTask.isCanceled) {
                msg = evaluate(interactTask, expression)

                if (msg == null || msg == false) {
                    delay(delayMillis)
                }
            }
            message = msg
        } finally {
            if (message == null) {
                if (!fetchTask.isCanceled && !interactTask.driver.isQuit && isActive) {
                    logger.warn("Timeout to wait for document ready after ${i.dec()} round, " +
                            "retry is supposed | {}", interactTask.url)
                    status = ProtocolStatus.retry(RetryScope.PRIVACY)
                    result.state = FlowState.BREAK
                }
            } else if (message == "timeout") {
                // this will never happen since 1.10.0
                logger.debug("Hit max round $maxRound to wait for document | {}", interactTask.url)
            } else if (message is String && message.contains("chrome-error://")) {
                val browserError = responseHandler.createBrowserError(message)
                status = browserError.status
                result.activeDOMMessage = browserError.activeDOMMessage
                result.state = FlowState.BREAK
            } else {
                if (tracer != null) {
                    val page = interactTask.fetchTask.page
                    val truncatedMessage = message.toString().substringBefore("urls")
                    tracer?.trace("{}. DOM is ready after {} evaluation | {}", page.id, i, truncatedMessage)
                }
            }
        }

        result.protocolStatus = status
    }

    protected open suspend fun scrollDown(interactTask: InteractTask, result: InteractResult) {
        val interactSettings = interactTask.interactSettings
        val random = ThreadLocalRandom.current().nextInt(3)
        val scrollDownCount = (interactSettings.scrollCount + random - 1).coerceAtLeast(1)
        val scrollInterval = interactSettings.scrollInterval.toMillis()

        val expressions = listOf(0.2, 0.3, 0.5, 0.75, 0.5, 0.4)
            .map { "__pulsar_utils__.scrollToMiddle($it)" }
            .toMutableList()
        repeat(scrollDownCount) {
            expressions.add("__pulsar_utils__.scrollDown()")
        }
        evaluate(interactTask, expressions, scrollInterval)
    }

    protected open suspend fun waitForElement(
        interactTask: InteractTask, requiredElements: List<String>
    ) {
        if (requiredElements.isNotEmpty()) {
            return
        }

        val expressions = requiredElements.map { "!!document.querySelector('$it')" }
        var scrollCount = 0

        val delayMillis = interactTask.interactSettings.scrollInterval.toMillis()
        var exists: Any? = null
        while (scrollCount-- > 0 && (exists == null || exists == false)) {
            counterJsWaits.inc()
            val verbose = false
            exists = expressions.all { expression -> true == evaluate(interactTask, expression, verbose) }
            delay(delayMillis)
        }
    }

    protected open suspend fun computeDocumentFeatures(interactTask: InteractTask, result: InteractResult) {
        val expression = "__pulsar_utils__.compute()"
        val message = evaluate(interactTask, expression)

        if (message is String) {
            result.activeDOMMessage = ActiveDOMMessage.fromJson(message)
            if (taskLogger.isDebugEnabled) {
                val page = interactTask.fetchTask.page
                taskLogger.debug("{}. {} | {}", page.id, result.activeDOMMessage?.trace, interactTask.url)
            }
        }
    }
}
