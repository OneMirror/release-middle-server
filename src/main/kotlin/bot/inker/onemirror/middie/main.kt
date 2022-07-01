package bot.inker.onemirror.middie

import io.undertow.Undertow
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("onemirror-middle")
fun main(args: Array<String>) {
    logger.info("Hello, server.")
    hibernate
    Undertow.builder()
        .addHttpListener(OneMirrorProperties["port"].toInt(), OneMirrorProperties["host"],
            OneMirrorRouteHandler
        )
        .build()
        .start()
    // PathResourceManager(Paths.get("/"),4096,true,true)
}