package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.BlobManager
import bot.inker.onemirror.middie.entity.ProjectDownloadEntity

class DownloadResource(
    val download: ProjectDownloadEntity
):Resource {
    override val isDirectory: Boolean = false
    override val name: String = download.name.substringAfterLast('/')
    override val lastModified: Long? = null
    override val size: Long? = download.hash?.let(BlobManager::getBlob)?.contentLength
    override val etag: String? = download.hash
    override val fileList: List<Resource> by lazy<List<Resource>>{ throw IllegalStateException("Not a directory") }
}