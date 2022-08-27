package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.simplify
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.avro.AvroRuntimeException
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DatumWriter
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.gora.memory.store.MemStore
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.jvm.Throws

/**
 * A very simple file backend storage for Web pages
 * */
class FileBackendPageStore(
        private val persistDirectory: Path = AppPaths.LOCAL_STORAGE_DIR
) : MemStore<String, GWebPage>() {

    private val logger = LoggerFactory.getLogger(FileBackendPageStore::class.java)
    private val unsafeConf = VolatileConfig.UNSAFE

    @Synchronized
    override fun get(reversedUrl: String, vararg fields: String): GWebPage? {
        var page = map[reversedUrl] as? GWebPage
        if (page == null) {
            page = readAvro(reversedUrl) ?: readHtml(reversedUrl)
        }
        return page
    }

    @Synchronized
    override fun put(reversedUrl: String, page: GWebPage) {
        super.put(reversedUrl, page)

        UrlUtils.unreverseUrlOrNull(reversedUrl)?.let {
            val p = WebPage.box(it, page, unsafeConf)
            writeAvro(p)
            writeHtml(p)
        }
    }

    override fun getSchemaName() = "FileBackendPageStore"

    override fun getFields(): Array<String> = GWebPage._ALL_FIELDS

    @Synchronized
    fun readHtml(reversedUrl: String): GWebPage? {
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl) ?: return null
        val filename = AppPaths.fromUri(url, "", ".htm")
        val path = persistDirectory.resolve(filename)

        logger.takeIf { it.isTraceEnabled }?.trace("Getting $reversedUrl $filename " + Files.exists(path))

        if (Files.exists(path)) {
            val content = Files.readAllBytes(path)
            // val lastModified = Files.getLastModifiedTime(path).toInstant()
            // never expire, so it serves as a mock site
            val lastModified = Instant.now()
            val page = newSuccessPage(url, lastModified, content)
            return page.unbox()
        }

        return null
    }

    @Synchronized
    fun readAvro(reversedUrl: String): GWebPage? {
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl) ?: return null
        val filename = AppPaths.fromUri(url, "", ".avro")
        val path = persistDirectory.resolve(filename)
        if (!Files.exists(path)) {
            return null
        }

        logger.takeIf { it.isTraceEnabled }?.trace("Getting $reversedUrl $filename " + Files.exists(path))
        return try {
            readAvro(path)
        } catch (e: AvroRuntimeException) {
            logger.warn("Failed to read avro file from $path, the file might be corrupted, delete it", e)
            Files.deleteIfExists(path)
            null
        } catch (e: IOException) {
            logger.warn(e.simplify("readAvro", " | $path"))
            Files.deleteIfExists(path)
            null
        }
    }

    @Synchronized
    fun readAvro(path: Path): GWebPage? {
        if (!Files.exists(path)) {
            return null
        }

        val datumReader: DatumReader<GWebPage> = SpecificDatumReader(GWebPage::class.java)
        var page: GWebPage? = null
        val dataFileReader: DataFileReader<GWebPage> = DataFileReader(path.toFile(), datumReader)
        dataFileReader.use {
            while (it.hasNext()) {
                page = it.next(page)
            }
        }
        return page
    }

    @Synchronized
    fun writeHtml(page: WebPage) {
        val filename = AppPaths.fromUri(page.url, "", ".htm")
        val path = persistDirectory.resolve(filename)

        logger.takeIf { it.isTraceEnabled }?.trace("Putting $filename ${page.content?.array()?.size}")

        // TODO: serialize with the metadata
        page.content?.let { Files.write(path, it.array()) }
    }

    @Synchronized
    fun writeAvro(page: WebPage) {
        val filename = AppPaths.fromUri(page.url, "", ".avro")
        val path = persistDirectory.resolve(filename)

        logger.takeIf { it.isTraceEnabled }?.trace("Putting $filename ${page.content?.array()?.size}")

        Files.deleteIfExists(path)
        try {
            writeAvro0(page.unbox(), path)
        } catch (e: AvroRuntimeException) {
            logger.warn("Failed to write avro file to $path", e)
        } catch (e: IOException) {
            logger.warn(e.simplify("writeAvro", " | $path"))
        }
    }

    @Throws(IOException::class)
    private fun writeAvro0(page: GWebPage, path: Path) {
        val datumWriter: DatumWriter<GWebPage> = SpecificDatumWriter(GWebPage::class.java)
        val dataFileWriter: DataFileWriter<GWebPage> = DataFileWriter(datumWriter)
        dataFileWriter.use {
            dataFileWriter.create(page.schema, path.toFile())
            dataFileWriter.append(page)
        }
    }

    private fun newSuccessPage(url: String, lastModified: Instant, content: ByteArray): WebPage {
        val page = WebPage.newWebPage(url, VolatileConfig.UNSAFE)
        page.also {
            it.location = url
            it.fetchCount = 1
            it.prevFetchTime = lastModified
            it.fetchInterval = ChronoUnit.DECADES.duration
            it.fetchTime = lastModified + it.fetchInterval
            it.crawlStatus = CrawlStatus.STATUS_FETCHED
            it.protocolStatus = ProtocolStatus.STATUS_SUCCESS
        }

        page.content = ByteBuffer.wrap(content)
        require(page.contentLength == content.size.toLong())
        require(page.persistContentLength == content.size.toLong())

        return page
    }
}
