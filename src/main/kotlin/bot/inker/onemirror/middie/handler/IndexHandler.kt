package bot.inker.onemirror.middie.handler

import bot.inker.onemirror.middie.resource.IndexResource
import bot.inker.onemirror.middie.util.ResourceRequestUtil
import bot.inker.onemirror.middie.util.RestResourceApi
import com.google.gson.stream.JsonWriter
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object IndexHandler {
    fun handle(exchange: HttpServerExchange) {
        when(exchange.requestMethod){
            Methods.GET -> {
                handleListProjects(exchange)
            }
        }
    }
    fun handleListProjects(exchange: HttpServerExchange){
        val depth = ResourceRequestUtil.parseDepth(exchange)
        val out = ByteArrayOutputStream()
        JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer->
            RestResourceApi.list(writer, IndexResource(), depth)
        }
        exchange.statusCode = 200
        exchange.responseSender.send(ByteBuffer.wrap(out.toByteArray()))
    }
}