package bot.inker.onemirror.middie.handler

import bot.inker.onemirror.middie.BlobManager
import bot.inker.onemirror.middie.repo.ProjectBuildRepo
import bot.inker.onemirror.middie.repo.ProjectDownloadRepo
import bot.inker.onemirror.middie.repo.ProjectRepo
import bot.inker.onemirror.middie.repo.ProjectVersionRepo
import bot.inker.onemirror.middie.request.PostDownloadRequest
import bot.inker.onemirror.middie.resource.DownloadResource
import bot.inker.onemirror.middie.transaction
import bot.inker.onemirror.middie.util.HandleResponseCode
import bot.inker.onemirror.middie.util.ResourceRequestUtil
import bot.inker.onemirror.middie.util.RestResourceApi
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods
import org.slf4j.LoggerFactory
import org.xnio.IoUtils
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.*

object ProjectDownloadHandler {
    private val logger = LoggerFactory.getLogger("onemirror-download-handler")
    private val gson = Gson()
    fun handle(exchange: HttpServerExchange, projectName:String, versionName:String, buildNumber: Int, fileName:String) {
        when(exchange.requestMethod){
            Methods.GET -> {
                handleListVersions(exchange,projectName,versionName, buildNumber, fileName)
            }
            Methods.POST -> {
                exchange.requestReceiver.receiveFullBytes{ exchange, message->
                    handlePost(exchange,
                        projectName,
                        versionName,
                        buildNumber,
                        fileName,
                        message.toString(StandardCharsets.UTF_8))
                }
            }
            Methods.PUT -> {
                uploadBlob(exchange){
                    handlePut(exchange, projectName, versionName, buildNumber, fileName, it)
                }
            }
        }
    }

    private fun handlePost(
        exchange: HttpServerExchange,
        projectName: String,
        versionName: String,
        buildNumber: Int,
        fileName: String,
        message: String,
    ){
        val request = gson.fromJson(message, PostDownloadRequest::class.java)
        val project = ProjectRepo.sync(projectName)
        val version = ProjectVersionRepo.sync(project, versionName)
        val build = ProjectBuildRepo.sync(project, version, buildNumber, request.commitMessage?:"No update message")
        val download = ProjectDownloadRepo.sync(project, version, build, fileName, request.hash)
        var updated = false
        if(request.status != null && download.status != download.status){
            download.status = download.status
            updated = true
        }
        if(updated){
            transaction { merge(build) }
        }
        HandleResponseCode.STATUS_NO_CONTENT.handleRequest(exchange)
    }
    private fun handlePut(
        exchange: HttpServerExchange,
        projectName: String,
        versionName: String,
        buildNumber: Int,
        fileName: String,
        blobHash: String,
    ) {
        val project = ProjectRepo.sync(projectName)
        val version = ProjectVersionRepo.sync(project, versionName)
        val build = ProjectBuildRepo.sync(project, version, buildNumber, "No update message")
        val download = ProjectDownloadRepo.sync(project, version, build, fileName, blobHash)
        HandleResponseCode.STATUS_NO_CONTENT.handleRequest(exchange)
    }

    private fun uploadBlob(exchange: HttpServerExchange,next:(String)->Unit) {
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
                next(BlobManager.createBlob(tmpPath, resultChecksum))
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

    fun handleListVersions(exchange: HttpServerExchange, projectName: String, versionName: String, buildNumber: Int, fileName:String){
        val depth = ResourceRequestUtil.parseDepth(exchange)
        val download = ProjectDownloadRepo.getByArguments(
            projectName = projectName,
            versionName = versionName,
            buildNumber = buildNumber,
            name = fileName
        ) ?:return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
        val out = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer->
            RestResourceApi.list(writer, DownloadResource(download), depth)
        }
        exchange.statusCode = 200
        exchange.responseSender.send(ByteBuffer.wrap(out.toByteArray()))
    }
}