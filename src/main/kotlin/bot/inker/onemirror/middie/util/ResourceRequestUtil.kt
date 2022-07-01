package bot.inker.onemirror.middie.util

import io.undertow.server.HttpServerExchange

object ResourceRequestUtil {
    private const val DEPTH_HEADER = "Depth"
    private const val DEPTH_PARAMETERS = "depth"
    fun parseDepth(exchange: HttpServerExchange):Int{
        val depthString = exchange.requestHeaders.getFirst(DEPTH_HEADER)
            ?: exchange.queryParameters.get(DEPTH_PARAMETERS)?.firstOrNull()
        if(depthString == null){
            return 1
        }
        if("Infinity".equals(depthString,true)){
            return Int.MAX_VALUE
        }
        return depthString.toInt()
    }
}