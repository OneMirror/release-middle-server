package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.entity.SyncStatus
import io.undertow.server.handlers.resource.Resource

interface StatusResource: Resource {
    val status:SyncStatus
}