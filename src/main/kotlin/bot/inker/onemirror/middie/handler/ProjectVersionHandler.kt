package bot.inker.onemirror.middie.handler

import bot.inker.onemirror.middie.repo.ProjectBuildRepo
import bot.inker.onemirror.middie.repo.ProjectRepo
import bot.inker.onemirror.middie.repo.ProjectVersionRepo
import bot.inker.onemirror.middie.request.PostBuildRequest
import bot.inker.onemirror.middie.resource.VersionResource
import bot.inker.onemirror.middie.transaction
import bot.inker.onemirror.middie.util.HandleResponseCode
import bot.inker.onemirror.middie.util.ResourceRequestUtil
import bot.inker.onemirror.middie.util.RestResourceApi
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object ProjectVersionHandler {
    private val gson = Gson()
    fun handle(exchange: HttpServerExchange, projectName:String, versionName:String) {
        when(exchange.requestMethod){
            Methods.GET -> {
                handleListVersions(exchange,projectName,versionName)
            }
            Methods.POST -> {
                exchange.requestReceiver.receiveFullBytes{ exchange, message->
                    handlePost(exchange, projectName, versionName, message.toString(StandardCharsets.UTF_8))
                }
            }
        }
    }

    private fun handlePost(
        exchange: HttpServerExchange,
        projectName: String,
        versionName: String,
        message: String
    ) {
        val request = gson.fromJson(message, PostBuildRequest::class.java)
        val project = ProjectRepo.sync(projectName)
        val version = ProjectVersionRepo.sync(project, versionName)
        var updated = false
        if(request.status != null && version.status != request.status){
            version.status = request.status
            updated = true
        }
        if(updated){
            transaction { merge(version) }
        }
        HandleResponseCode.STATUS_NO_CONTENT.handleRequest(exchange)
    }

    fun handleListVersions(exchange: HttpServerExchange, projectName: String, versionName: String){
        val depth = ResourceRequestUtil.parseDepth(exchange)
        val version = ProjectVersionRepo.getByArguments(
            projectName = projectName,
            name = versionName
        ) ?:return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
        val out = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer->
            RestResourceApi.list(writer, VersionResource(version), depth)
        }
        exchange.statusCode = 200
        exchange.responseSender.send(ByteBuffer.wrap(out.toByteArray()))
    }
}