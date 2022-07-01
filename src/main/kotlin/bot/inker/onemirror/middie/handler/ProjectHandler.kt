package bot.inker.onemirror.middie.handler

import bot.inker.onemirror.middie.repo.ProjectRepo
import bot.inker.onemirror.middie.request.PostBuildRequest
import bot.inker.onemirror.middie.resource.ProjectResource
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

object ProjectHandler {
    private val gson = Gson()
    fun handle(exchange: HttpServerExchange, projectName:String) {
        when(exchange.requestMethod){
            Methods.GET -> {
                handleListProjects(exchange,projectName)
            }
            Methods.POST -> {
                exchange.requestReceiver.receiveFullBytes{ exchange, message->
                    handlePost(exchange, projectName, message.toString(StandardCharsets.UTF_8))
                }
            }
        }
    }

    private fun handlePost(
        exchange: HttpServerExchange,
        projectName: String,
        message: String
    ) {
        val request = gson.fromJson(message, PostBuildRequest::class.java)
        val project = ProjectRepo.sync(projectName)
        var updated = false
        if(request.status != null && project.status != request.status){
            project.status = request.status
            updated = true
        }
        if(updated){
            transaction { merge(project) }
        }
        HandleResponseCode.STATUS_NO_CONTENT.handleRequest(exchange)
    }

    fun handleListProjects(exchange: HttpServerExchange, projectName:String){
        val depth = ResourceRequestUtil.parseDepth(exchange)
        val project = ProjectRepo.getByArguments(name = projectName)
            ?:return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
        val out = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer->
            RestResourceApi.list(writer, ProjectResource(project), depth)
        }
        exchange.statusCode = 200
        exchange.responseSender.send(ByteBuffer.wrap(out.toByteArray()))
    }
}