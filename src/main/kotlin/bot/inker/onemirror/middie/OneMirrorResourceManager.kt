package bot.inker.onemirror.middie

import bot.inker.onemirror.middie.entity.*
import bot.inker.onemirror.middie.repo.ProjectBuildRepo
import bot.inker.onemirror.middie.repo.ProjectDownloadRepo
import bot.inker.onemirror.middie.repo.ProjectRepo
import bot.inker.onemirror.middie.repo.ProjectVersionRepo
import bot.inker.onemirror.middie.resource.DefaultedResource
import io.undertow.io.IoCallback
import io.undertow.io.Sender
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.RangeAwareResource
import io.undertow.server.handlers.resource.Resource
import io.undertow.server.handlers.resource.ResourceSupplier
import io.undertow.util.ETag
import org.xnio.IoUtils
import java.io.IOException

class OneMirrorResourceManager:ResourceSupplier{
    override fun getResource(exchange: HttpServerExchange, path: String): Resource? { session {
        val elements = path.split('/')
            .filter{ it.isNotEmpty() && it.isNotBlank() }

        if(elements.size == 0){
            return ProjectListResource()
        }

        val project = ProjectRepo.getByArguments(
            name = elements[0]
        ) ?: return null
        if(elements.size == 1){
            return ProjectResource(project)
        }

        val version = ProjectVersionRepo.getByArguments(
            project = project,
            name = elements[1]
        ) ?: return null
        if(elements.size == 2){
            return VersionResource(project, version)
        }

        val build = ProjectBuildRepo.getByArguments(
            project = project,
            version = version,
            buildNumber = elements[2].toIntOrNull() ?: return null
        ) ?: return null
        if(elements.size == 3){
            return BuildResource(project, version, build)
        }

        val fileName = elements.subList(3,elements.size).joinToString("/")
        val download = ProjectDownloadRepo.getAll(build).filter {
            it.name == fileName
        }.singleOrNull() ?: return null
        val downloadResource = DownloadResource(project, version, build, download)
        if (downloadResource.blob == null) {
            return null
        }else{
            return downloadResource
        }
    } }

    class ProjectListResource: DefaultedResource {
        override fun getPath() = "/"
        override fun getName() = "/"
        override fun isDirectory() = true
        private val list by lazy { session { ProjectRepo.getAll() }.map{ ProjectResource(it) } }
        override fun list() = list
        override val status = SyncStatus.SUCCESS
    }

    class ProjectResource(
        val project: ProjectEntity,
    ): DefaultedResource {
        override fun getPath() = "/${project.name}/"
        override fun getName() = project.name
        override fun isDirectory() = true
        private val list by lazy { session { ProjectVersionRepo.getAll(project) }.map{ VersionResource(project, it) } }
        override fun list() = list
        override val status = project.status
    }

    class VersionResource(
        val project: ProjectEntity,
        val version: ProjectVersionEntity,
    ): DefaultedResource {
        override fun getPath() = "/${project.name}/${version.name}/"
        override fun getName() = version.name
        override fun isDirectory() = true
        private val list by lazy { session { ProjectBuildRepo.getAll(version) }.map{ BuildResource(project, version, it) } }
        override fun list() = list
        override val status = version.status
    }

    class BuildResource(
        val project: ProjectEntity,
        val version: ProjectVersionEntity,
        val build: ProjectBuildEntity,
    ): DefaultedResource {
        override fun getPath() = "/${project.name}/${version.name}/${build.buildNumber}/"
        override fun getName() = build.buildNumber.toString()
        override fun isDirectory() = true
        private val list by lazy {
            session { ProjectDownloadRepo.getAll(build) }
                .map{ DownloadResource(project, version, build, it) }
        }
        override fun list() = list
        override val status = build.status
    }

    class DownloadResource(
        val project: ProjectEntity,
        val version: ProjectVersionEntity,
        val build: ProjectBuildEntity,
        val download: ProjectDownloadEntity,
    ): DefaultedResource,RangeAwareResource{
        override fun getPath() = "/${project.name}/${version.name}/${build.buildNumber}/${download.name}"
        override fun getName() = download.name
        override fun isDirectory() = false
        private val etag = download.hash?.let { ETag(false, it) }
        override fun getETag() = etag
        override fun getCacheKey() = download.hash

        val blob by lazy{ BlobManager.getBlob(download.hash) }

        override fun serve(sender: Sender, exchange: HttpServerExchange, completionCallback: IoCallback) {
            serveRange(sender, exchange, 0, 0, completionCallback)
        }

        override fun serveRange(
            sender: Sender,
            exchange: HttpServerExchange,
            start: Long,
            end: Long,
            callback: IoCallback,
        ) {
            val blob = blob as BlobManager.Blob.FileSystem
            val fileChannel = blob.channel()
            fileChannel.position(start)
            sender.transferFrom(fileChannel, object : IoCallback {
                override fun onComplete(exchange: HttpServerExchange, sender: Sender) {
                    try {
                        IoUtils.safeClose(fileChannel)
                    } finally {
                        callback.onComplete(exchange, sender)
                    }
                }

                override fun onException(exchange: HttpServerExchange, sender: Sender, exception: IOException) {
                    try {
                        IoUtils.safeClose(fileChannel)
                    } finally {
                        callback.onException(exchange, sender, exception)
                    }
                }
            })
        }

        @get:JvmName("\$contentLength")
        private val contentLength by lazy {
            blob?:return@lazy null
            (blob as BlobManager.Blob).contentLength
        }
        override fun getContentLength() = contentLength
        override fun isRangeSupported() = (blob is BlobManager.Blob.WithRandomAccess)
    }
}