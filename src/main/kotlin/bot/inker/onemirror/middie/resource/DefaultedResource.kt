package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.entity.SyncStatus
import io.undertow.io.IoCallback
import io.undertow.io.Sender
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.Resource
import io.undertow.util.ETag
import io.undertow.util.MimeMappings
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.util.Date

interface DefaultedResource:StatusResource {
    override fun getLastModified():Date? = null
    override fun getLastModifiedString():String? = null
    override fun getETag(): ETag? = null
    override fun list():List<Resource> = emptyList()
    override fun getContentType(mimeMappings: MimeMappings):String? = null
    override fun serve(sender: Sender, exchange: HttpServerExchange, completionCallback: IoCallback)
        = completionCallback.onComplete(exchange, sender)
    override fun getContentLength():Long? = null
    override fun getCacheKey():String? = null
    override fun getFile(): File? = null
    override fun getFilePath():Path? = null
    override fun getResourceManagerRoot(): File? = null
    override fun getResourceManagerRootPath():Path? = null
    override fun getUrl():URL? = null
    override val status: SyncStatus
        get() = SyncStatus.SUCCESS
}