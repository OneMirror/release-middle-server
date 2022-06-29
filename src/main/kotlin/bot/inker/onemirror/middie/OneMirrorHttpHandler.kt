package bot.inker.onemirror.middie

import bot.inker.onemirror.middie.directory.OneDirectoryUtils
import bot.inker.onemirror.middie.directory.OneMirrorFileListPage
import bot.inker.onemirror.middie.repo.ProjectBuildRepo
import bot.inker.onemirror.middie.repo.ProjectDownloadRepo
import bot.inker.onemirror.middie.repo.ProjectRepo
import bot.inker.onemirror.middie.repo.ProjectVersionRepo
import bot.inker.onemirror.middie.util.HandleResponseCode
import io.undertow.io.IoCallback
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.RangeAwareResource
import io.undertow.server.handlers.resource.Resource
import io.undertow.server.handlers.resource.ResourceSupplier
import io.undertow.util.*
import org.slf4j.LoggerFactory
import org.xnio.IoUtils
import java.nio.file.Files
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

// TODO: split it
class OneMirrorHttpHandler(
    val resourceSupplier: ResourceSupplier,
    page: OneMirrorFileListPage,
) : HttpHandler {
    private val logger = LoggerFactory.getLogger("onemirror-http-handler")
    private val page = OneDirectoryUtils(page)

    override fun handleRequest(exchange: HttpServerExchange) {
        when (exchange.requestMethod) {
            Methods.GET, Methods.HEAD -> {
                serveResource(exchange, exchange.requestMethod == Methods.GET)
            }
            Methods.PUT -> {
                uploadBlob(exchange)
            }
            Methods.POST ->{

            }
            else -> {
                exchange.responseHeaders.add(Headers.ALLOW,
                    arrayOf(Methods.GET_STRING, Methods.HEAD_STRING).joinToString(", ")
                )
                HandleResponseCode.STATUS_METHOD_NOT_ALLOWED.handleRequest(exchange)
            }
        }
    }

    private fun uploadBlob(exchange: HttpServerExchange) {
        // TODO: Create download entity before receive content
        val exceptChecksum = exchange.requestHeaders.getFirst("Checksum")
        val tmpPath = Files.createTempFile("onemirror-upload-", ".tmp")
        val fileOut = Files.newOutputStream(tmpPath)
        val digest = MessageDigest.getInstance("SHA-256")
        val receiver = exchange.requestReceiver
        receiver.setMaxContentSize(-1)
        exchange.requestReceiver.receivePartialBytes({ _, message, last ->
            digest.update(message)
            fileOut.write(message)
            if (last) {
                fileOut.close()
                val resultChecksum = digest.digest().let(HexFormat.of()::formatHex)
                if(exceptChecksum != null && !exceptChecksum.equals(resultChecksum, true)){
                    HandleResponseCode.STATUS_BAD_REQUEST.handleRequest(exchange)
                    return@receivePartialBytes
                }
                BlobManager.createBlob(tmpPath, resultChecksum)
                createResource(exchange, resultChecksum)
                // HandleResponseCode.STATUS_NO_CONTENT.handleRequest(exchange)
            }
        },{ _, e->
            logger.warn("Failed handle upload resource")
            IoUtils.safeClose(fileOut)
            IoUtils.safeClose { Files.deleteIfExists(tmpPath) }
            if (!exchange.isResponseStarted) {
                HandleResponseCode.STATUS_INTERNAL_SERVER_ERROR.handleRequest(exchange)
            }
        })
    }

    private fun createResource(exchange: HttpServerExchange, fileHash:String){
        val changeList = exchange.requestHeaders.get("Change-List")
            ?.joinToString("\n")
            ?: "No update message"
        val path = processPath(exchange)?:return HandleResponseCode.STATUS_BAD_REQUEST.handleRequest(exchange)
        if(path.isEmpty()){
            return HandleResponseCode.STATUS_BAD_REQUEST.handleRequest(exchange)
        }
        val splits = path.substring(1).split('/')
        if (splits.size < 4) {
            return HandleResponseCode.STATUS_BAD_REQUEST.handleRequest(exchange)
        }
        val projectName = splits[0]
        val versionName = splits[1]
        val buildNumber = splits[2].toIntOrNull()?:return HandleResponseCode.STATUS_BAD_REQUEST.handleRequest(exchange)
        val fileName = splits.subList(3,splits.size).joinToString("/")
        val project = ProjectRepo.sync(projectName)
        val version = ProjectVersionRepo.sync(project, versionName)
        val build = ProjectBuildRepo.sync(project, version, buildNumber, changeList)
        val download = ProjectDownloadRepo.sync(
            project, version, build, fileName, fileHash
        )
        HandleResponseCode.STATUS_NO_CONTENT.handleRequest(exchange)
    }

    private fun serveResource(exchange: HttpServerExchange, sendContent: Boolean) {
        // TODO: Cache resources' contentLength etag and more
        // TODO: Don't handle it in io thread

        // Check and handle if request page blobs (when exchange.queryString is "js" "css")
        if (page.sendRequestedBlobs(exchange)) {
            return
        }

        // Normalize path (defence attacks likes all use "\")
        val resolvedPath = processPath(exchange)
            ?: return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)

        // Get resource from resource supplier
        val resource = resourceSupplier.getResource(exchange, resolvedPath)
            ?: return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)

        // Check and handle if request json (exchange.queryString=="json")
        if (page.sendRequestedJson(exchange, resource)) {
            return
        }

        // If is a directory, show directory list
        if (resource.isDirectory) {
            return page.renderDirectoryListing(exchange, resource)
        }

        // Return STATUS_NOT_FOUND when resource not a directory but request a directory
        if (resolvedPath.endsWith('/')) {
            return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
        }

        // Check and handle if client cache is still valid
        if (processCache(exchange, resource.eTag, resource.lastModified)) {
            return
        }

        // put content length if resource support it
        if (resource.contentLength != null) {
            exchange.responseContentLength = resource.contentLength
        }

        // Process RangeAwareResource and handle REQUEST_RANGE_NOT_SATISFIABLE
        val rangeResponse = processRangeAwareResource(exchange, resource)
        if (rangeResponse != null && rangeResponse.statusCode == StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
            return
        }

        putCacheTimeHeader(exchange, resource.lastModifiedString, resource.eTag?.toString())

        if (!sendContent) {
            exchange.endExchange()
        } else if (rangeResponse != null) {
            (resource as RangeAwareResource).serveRange(exchange.responseSender,
                exchange,
                rangeResponse.start,
                rangeResponse.end,
                IoCallback.END_EXCHANGE
            )
        } else {
            resource.serve(exchange.responseSender, exchange, IoCallback.END_EXCHANGE)
        }
    }

    private fun processPath(exchange: HttpServerExchange): String? {
        return exchange.relativePath.also {
            if (!it.contains('/')) {
                return null
            }
        }.also {
            if (it.contains('\\')) {
                return null
            }
        }.let {
            CanonicalPathUtils.canonicalize(it)
        }
    }

    private fun processRangeAwareResource(exchange: HttpServerExchange, resource:Resource): ByteRange.RangeResponseResult? {
        if (resource is RangeAwareResource && resource.isRangeSupported && resource.contentLength != null) {
            exchange.responseHeaders.put(Headers.ACCEPT_RANGES, "bytes")
            val range = ByteRange.parse(exchange.requestHeaders.getFirst(Headers.RANGE))
            if (range != null && range.ranges == 1 && resource.getContentLength() != null) {
                val responseResult = range.getResponseResult(
                    resource.getContentLength(),
                    exchange.requestHeaders.getFirst(Headers.IF_RANGE),
                    resource.lastModified,
                    resource.eTag?.tag
                )?:return null

                exchange.statusCode = responseResult.statusCode
                exchange.responseHeaders.put(Headers.CONTENT_RANGE, responseResult.contentRange)
                exchange.responseContentLength = responseResult.contentLength

                if (responseResult.statusCode == StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE) {
                    HandleResponseCode.STATUS_REQUEST_RANGE_NOT_SATISFIABLE.handleRequest(exchange)
                }
            }
        }
        return null
    }

    private fun processCache(exchange: HttpServerExchange, etag: ETag?, lastModified: Date?): Boolean {
        if (!ETagUtils.handleIfMatch(exchange, etag, false) ||
            !DateUtils.handleIfUnmodifiedSince(exchange, lastModified)
        ) {
            HandleResponseCode.STATUS_PRECONDITION_FAILED.handleRequest(exchange)
            exchange.endExchange()
            return true
        }
        if (!ETagUtils.handleIfNoneMatch(exchange, etag, true) ||
            !DateUtils.handleIfModifiedSince(exchange, lastModified)
        ) {
            HandleResponseCode.STATUS_NOT_MODIFIED.handleRequest(exchange)
            exchange.endExchange()
            return true
        }
        return false
    }

    private val cacheTime = "31536000"
    private fun putCacheTimeHeader(exchange: HttpServerExchange, lastModifiedString: String?, etag: String?) {
        exchange.responseHeaders.put(Headers.CACHE_CONTROL,
            "public, max-age=$cacheTime")
        val date = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cacheTime.toLong())
        val dateHeader = DateUtils.toDateString(Date(date))
        exchange.responseHeaders.put(Headers.EXPIRES, dateHeader)
        lastModifiedString?.let { exchange.responseHeaders.put(Headers.LAST_MODIFIED, it) }
        etag?.let { exchange.responseHeaders.put(Headers.ETAG, it) }
    }
}