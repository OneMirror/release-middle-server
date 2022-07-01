package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.entity.ProjectBuildEntity
import bot.inker.onemirror.middie.repo.ProjectDownloadRepo

class BuildResource(
    val build: ProjectBuildEntity
):Resource {
    override val isDirectory: Boolean = true
    override val name: String = build.buildNumber.toString()
    override val lastModified: Long? = null
    override val size: Long? = null
    override val etag: String? = null
    override val fileList: List<Resource> by lazy {
        ProjectDownloadRepo.getAll(build)
            .map(::DownloadResource)
    }
}