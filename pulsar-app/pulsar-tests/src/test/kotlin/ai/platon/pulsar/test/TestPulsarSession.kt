package ai.platon.pulsar.test

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.persist.WebPage
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoField
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestPulsarSession: TestBase() {
    private val url = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty"

    @Test
    fun testNormalize() {
        val normUrl = session.normalize(url)
        assertNotEquals(session.sessionConfig, normUrl.options.conf)
        val page = session.load(normUrl)
        assertEquals(normUrl.options.conf, page.conf)
    }

    @Test
    fun testFetchForExpires() {
        val seconds = 5L
        val args = "-i ${seconds}s"
        var startTime = Instant.now()
        println("Start time: $startTime")

        val page = session.load(url, args = args)
        val prevFetchTime1 = page.prevFetchTime
        val fetchTime1 = page.fetchTime
        val fetchCount1 = page.fetchCount

        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.isFetched }
        assertTrue { page.isContentUpdated }
        assertTrue("prevFetchTime: $prevFetchTime1 startTime: $startTime") { startTime < prevFetchTime1 }
        assertTrue("prevFetchTime: $prevFetchTime1 startTime: $startTime") { prevFetchTime1 < fetchTime1 }
        assertEquals(seconds, page.fetchInterval.seconds)
        assertTrue { page.fetchTime > startTime }
        assertEquals(page.prevFetchTime.plusSeconds(seconds), page.fetchTime)
        assertTrue { page.fetchTime > page.prevFetchTime }
        assertTrue { page.fetchCount > 1 }

        sleepSeconds(6)
        startTime = Instant.now()
        val page2 = session.load(url, args = args)
        val prevFetchTime2 = page2.prevFetchTime
        val fetchTime2 = page2.fetchTime
        val fetchCount2 = page2.fetchCount

        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page2.isFetched }
        assertTrue { page2.isContentUpdated }
        assertTrue { prevFetchTime2 > startTime }
        assertTrue { prevFetchTime2 > prevFetchTime1 }
        assertTrue { fetchTime2 > startTime }
        assertTrue { fetchTime2 > page2.prevFetchTime }
        assertEquals(fetchCount1 + 1, fetchCount2)
    }

    @Test
    fun testFetchForExpireAt() {
        val now = Instant.now()
        val seconds = 5L
        var options = session.options()
        var startTime = Instant.now()
        println("Start time: $startTime")

        val page = session.load(url, options)
        val prevFetchTime1 = page.prevFetchTime
        val fetchTime1 = page.fetchTime
        val fetchCount1 = page.fetchCount

        println("Round 1 checking ....")
        assertTrue("${page.protocolStatus}") { page.protocolStatus.isSuccess }
        assertFalse("Should be loaded") { page.isFetched }
        assertFalse("Content should not be updated") { page.isContentUpdated }
        assertTrue { page.options.expires.seconds > 0 }
        assertTrue("Not expired, prevFetchTime should be before startTime: $prevFetchTime1 -> $startTime") {
            startTime > prevFetchTime1
        }
        assertTrue("prevFetchTime: $prevFetchTime1 fetchTime1: $fetchTime1") {
            prevFetchTime1 < fetchTime1
        }
        assertTrue { page.fetchTime > startTime }
        // since expireAt is used
//        assertEquals(prevFetchTime1.plusSeconds(seconds), page.fetchTime)
        assertTrue { page.fetchTime > page.prevFetchTime }
        assertTrue { page.fetchCount > 1 }
        options = session.options().apply { expireAt = now.plusSeconds(seconds) }

        println("Wait for 6 seconds so the page is expired .... | $options")
        sleepSeconds(6)
        startTime = Instant.now()
        assertTrue("expireAt: ${options.expireAt} startTime: $startTime") {
            options.expireAt < startTime
        }
        assertTrue("Should be expired at ${options.expireAt} <- $now") {
            options.isExpired(page.prevFetchTime)
        }
        val page2 = session.load(url, options)
        val prevFetchTime2 = page2.prevFetchTime
        val fetchTime2 = page2.fetchTime
        val fetchCount2 = page2.fetchCount

        println("Round 2 checking ....")
        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page2.isFetched }
        assertTrue { page2.isContentUpdated }
        assertTrue { prevFetchTime2 > startTime }
        assertTrue("prevFetchTime1: $prevFetchTime1 prevFetchTime2: $prevFetchTime2") {
            prevFetchTime2 > prevFetchTime1
        }
        assertTrue { fetchTime2 > startTime }
        assertTrue { fetchTime2 > prevFetchTime2 }
        assertEquals(fetchCount1 + 1, fetchCount2)
    }
}
