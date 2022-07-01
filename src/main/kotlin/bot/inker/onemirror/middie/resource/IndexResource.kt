package bot.inker.onemirror.middie.resource

import bot.inker.onemirror.middie.repo.ProjectRepo

class IndexResource : Resource {
    override val isDirectory: Boolean = true
    override val name: String = "/"
    override val lastModified: Long? = null
    override val size: Long? = null
    override val etag: String? = null
    override val fileList: List<Resource> by lazy {
        ProjectRepo.getAllProjects()
            .map(::ProjectResource)
    }
}