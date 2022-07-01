package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.entity.ProjectVersionEntity
import bot.inker.onemirror.middie.repo.ProjectBuildRepo

class VersionResource(
    val version: ProjectVersionEntity
):Resource {
    override val isDirectory: Boolean = true
    override val name: String = version.name
    override val lastModified: Long? = null
    override val size: Long? = null
    override val etag: String? = null
    override val fileList: List<Resource> by lazy {
        ProjectBuildRepo.getAll(version)
            .map(::BuildResource)
    }
}