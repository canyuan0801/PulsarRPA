package ai.platon.pulsar.examples

import ai.platon.pulsar.context.PulsarContexts
import com.google.gson.Gson

fun main() {
    // create a pulsar session
    val session = PulsarContexts.createSession()
    // the main url we are playing with
    val url = "https://list.jd.com/list.html?cat=670,671,12798"
    // load a page, or fetch it from the Internet if it does not exist or has expired
    val page = session.load(url, "-expires 10s")
    // parse the page content into a Jsoup document
    val document = session.parse(page)
    // do something with the document
    // ...

    // or, load and parse
    val document2 = session.loadDocument(url, "-expires 10s")
    // do something with the document
    // ...

    // load all pages with links specified by -outLink
    val pages = session.loadOutPages(url, "-expires 10s -itemExpires 10s -outLink a[href~=item]")
    // load the portal page and submit the out links specified by the `-outLink` option to the URL pool
    session.submitOutPages(url, "-expires 1d -itemExpires 7d -outLink a[href~=item]")
    // load, parse and scrape fields
    val fields = session.scrape(url, "-expires 10s", "li[data-sku]", listOf(".p-name em", ".p-price"))
    // load, parse and scrape named fields
    val fields2 = session.scrape(url, "-i 10s", "li[data-sku]", mapOf("name" to ".p-name em", "price" to ".p-price"))
    // load, parse and scrape named fields
    val fields3 = session.scrapeOutPages(url, "-i 10s -ii 10s", "li[data-sku]", mapOf("name" to ".sku-name", "price" to ".p-price"))

    println("== document")
    println(document.title)
    println(document.selectFirstOrNull("title")?.text())

    println("== document2")
    println(document2.title)
    println(document2.selectFirstOrNull("title")?.text())

    println("== pages")
    println(pages.map { it.url })

    val gson = Gson()
    println("== fields")
    println(gson.toJson(fields))

    println("== fields2")
    println(gson.toJson(fields2))

    println("== fields3")
    println(gson.toJson(fields3))
}
