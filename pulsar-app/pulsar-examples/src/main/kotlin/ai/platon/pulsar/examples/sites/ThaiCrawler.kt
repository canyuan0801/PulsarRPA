package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.context.PulsarContexts

fun main() {
    val portalUrl = "https://shopee.co.th/กระเป๋าเป้ผู้ชาย-cat.49.1037.10297?page=1"
    val args = """
        -i 1s -ii 1s -ol ".shopee-search-item-result__item a" -sc 10
    """.trimIndent()

    val session = PulsarContexts.createSession()
    session.loadOutPages(portalUrl, args)
}
