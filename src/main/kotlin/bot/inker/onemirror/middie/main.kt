package bot.inker.onemirror.middie

import bot.inker.onemirror.middie.directory.OneMirrorFileListPage
import io.undertow.Undertow
import io.undertow.server.handlers.resource.DefaultResourceSupplier
import io.undertow.server.handlers.resource.DirectoryUtils
import io.undertow.server.handlers.resource.PathResourceManager
import org.slf4j.LoggerFactory
import org.xnio.XnioWorker
import java.nio.file.Paths

private val logger = LoggerFactory.getLogger("onemirror-middle")
fun main(args: Array<String>) {
    logger.info("Hello, server.")
    hibernate
    Undertow.builder()
        .addHttpListener(MiddleProperties["port"].toInt(), MiddleProperties["host"], OneMirrorHttpHandler(
            OneMirrorResourceManager(),
            OneMirrorFileListPage()
        ))
        .build()
        .start()
    // PathResourceManager(Paths.get("/"),4096,true,true)
}