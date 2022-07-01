package bot.inker.onemirror.middie

import bot.inker.onemirror.middie.handler.*
import bot.inker.onemirror.middie.util.HandleResponseCode
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.CanonicalPathUtils
import io.undertow.util.Headers
import io.undertow.util.StatusCodes

object OneMirrorRouteHandler : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        val path = processPath(exchange)
            ?: return HandleResponseCode.STATUS_BAD_REQUEST.handleRequest(exchange)
        var elements = path.substring(1).split('/')
        val isDirectory = elements.last().isEmpty()
        if (isDirectory) {
            elements = elements.subList(0, elements.size-1)
        }
        if(isDirectory){
            when(elements.size) {
                0 -> IndexHandler.handle(exchange)
                1 -> {
                    ProjectHandler.handle(exchange, elements[0])
                }
                2 -> {
                    ProjectVersionHandler.handle(exchange, elements[0], elements[1])
                }
                3 -> {
                    val buildNumber = elements[2].toIntOrNull()
                        ?: return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
                    ProjectBuildHandler.handle(exchange, elements[0], elements[1], buildNumber)
                }
                else -> {
                    return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
                }
            }
        }else{
            when(elements.size){
                0 -> return location(exchange,"/")
                1 -> return location(exchange,"/${elements[0]}/")
                2 -> return location(exchange,"/${elements[0]}/${elements[1]}/")
                3 -> return location(exchange,"/${elements[0]}/${elements[1]}/${elements[2]}/")
                else -> { /* > 3 */
                    if(isDirectory){
                        return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
                    }
                    val buildNumber = elements[2].toIntOrNull()
                        ?:return HandleResponseCode.STATUS_NOT_FOUND.handleRequest(exchange)
                    val fileName = elements.subList(3,elements.size).joinToString("/")
                    ProjectDownloadHandler.handle(exchange, elements[0], elements[1], buildNumber, fileName)
                }
            }
        }
    }
    private fun processPath(exchange: HttpServerExchange): String? {
        val path = exchange.relativePath
        if(!path.contains('/')){
            return null
        }
        if (path.contains('\\')) {
            return null
        }
        return CanonicalPathUtils.canonicalize(path)
    }
    private fun location(exchange: HttpServerExchange, path:String){
        exchange.statusCode = StatusCodes.MOVED_PERMANENTLY
        exchange.responseHeaders.put(Headers.LOCATION, path)
        exchange.endExchange()
    }
}