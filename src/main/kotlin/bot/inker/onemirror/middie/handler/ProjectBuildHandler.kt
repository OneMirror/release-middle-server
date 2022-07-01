package bot.inker.onemirror.middie.handler

import bot.inker.onemirror.middie.repo.ProjectBuildRepo
import bot.inker.onemirror.middie.repo.ProjectRepo
import bot.inker.onemirror.middie.repo.ProjectVersionRepo
import bot.inker.onemirror.middie.request.PostBuildRequest
import bot.inker.onemirror.middie.resource.BuildResource
import bot.inker.onemirror.middie.transaction
import bot.inker.onemirror.middie.util.HandleResponseCode
import bot.inker.onemirror.middie.util.ResourceRequestUtil
import bot.inker.onemirror.middie.util.RestResourceApi
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object ProjectBuildHandler {
    private val gson = Gson()
    fun handle(exchange: HttpServerExchange, projectName:String, versionName:String, buildNumber: Int) {
        when(exchange.requestMethod){
            Methods.GET -> {
                handleListVersions(exchange,projectName,versionName, buildNumber)
            }
            Methods.POST -> {
                exchange.requestReceiver.receiveFullBytes{ exchange, message->
                    handlePost(exchange, projectName, versionName, buildNumber, message.toString(StandardCharsets.UTF_8))
                }
            }
        }
    }

    private fun handlePost(
        exchange: HttpServerExchange,
        projectName: String,
        versionName: String,
        buildNumber: Int,
        message: String
    ) {
        val buildNumber = if(buildNumber != 0){
            buildNumber
        } else {
            ProjectBuildRepo.getNextBuildNumber()
        }
        val request = gson.fromJson(message, PostBuildRequest::class.java)
        val project = ProjectRepo.sync(projectName)
        val version = ProjectVersionRepo.sync(project, versionName)
        val build = ProjectBuildRepo.sync(
            project,
            version,
            buildNumber,
            request.commitMessage?:"No update message"
        )
        var updated = false
        if(request.status != null && build.status != request.status){
            build.status = request.status
            updated = true
        }
        if(updated){
            transaction { merge(build) }
        }
        exchange.statusCode = 200
        exchange.responseSender.send(
            ByteBuffer.wrap(
                buildNumber.toString()
                    .toByteArray(StandardCharsets.UTF_8)
            )
        )
    }

    fun handleListVersions(exchange: HttpServerExchange, projectName: String, versionName: String, buildNumber: Int){
        val depth = ResourceRequestUtil.parseDepth(exchange)
        val build = ProjectBuildRepo.getByArguments(
            projectName = projectName,
            versionName = versionName,
            buildNumber = buildNumber
        ) ?:return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
        val out = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer->
            RestResourceApi.list(writer, BuildResource(build), depth)
        }
        exchange.statusCode = 200
        exchange.responseSender.send(ByteBuffer.wrap(out.toByteArray()))
    }
}