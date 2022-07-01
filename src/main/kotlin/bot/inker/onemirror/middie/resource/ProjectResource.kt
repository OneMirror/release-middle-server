package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.entity.ProjectEntity
import bot.inker.onemirror.middie.repo.ProjectVersionRepo

class ProjectResource(
    val project:ProjectEntity
):Resource {
    override val isDirectory: Boolean = true
    override val name: String = project.name
    override val lastModified: Long? = null
    override val size: Long? = null
    override val etag: String? = null
    override val fileList: List<Resource> by lazy {
        ProjectVersionRepo.getAll(project)
            .map(::VersionResource)
    }
}