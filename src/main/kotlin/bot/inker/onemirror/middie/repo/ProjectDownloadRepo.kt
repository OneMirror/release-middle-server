package bot.inker.onemirror.middie.repo

import bot.inker.onemirror.middie.entity.*
import bot.inker.onemirror.middie.session
import bot.inker.onemirror.middie.transaction
import jakarta.persistence.criteria.Predicate
import org.slf4j.LoggerFactory

object ProjectDownloadRepo {
    private val logger = LoggerFactory.getLogger("onemirror-download")
    fun getAll(build: ProjectBuildEntity): List<ProjectDownloadEntity> {
        session {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectDownloadEntity::class.java).apply {
                    val root = from(ProjectDownloadEntity::class.java)
                    where(equal(root.get<ProjectBuildEntity>("build"), build))
                }
            }).resultList
        }
    }

    private fun create(
        project: ProjectEntity,
        version: ProjectVersionEntity,
        build: ProjectBuildEntity,
        name: String,
        hash: String?,
        downloadUrl: String?,
    ): ProjectDownloadEntity {
        transaction {
            return ProjectDownloadEntity().apply {
                this.project = project
                this.version = version
                this.build = build
                this.name = name
                this.hash = hash
                this.downloadUrl = downloadUrl
            }.also {
                persist(it)
            }
        }
    }

    fun getByArguments(
        project: ProjectEntity? = null,
        projectName: String? = null,
        version: ProjectVersionEntity? = null,
        versionName: String? = null,
        build: ProjectBuildEntity? = null,
        buildNumber: Int? = null,
        name: String? = null,
        hash: String? = null,
        downloadUrl: String? = null,
    ): ProjectDownloadEntity? {
        transaction {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectDownloadEntity::class.java).apply {
                    val root = from(ProjectDownloadEntity::class.java)
                    where(*ArrayList<Predicate>().apply {
                        project?.let { add(equal(root.get<ProjectEntity>("project"), it)) }
                        projectName?.let { add(equal(root.get<ProjectEntity>("project").get<String>("name"), it)) }
                        version?.let { add(equal(root.get<ProjectVersionEntity>("version"), it)) }
                        projectName?.let {
                            add(equal(root.get<ProjectVersionEntity>("version").get<String>("name"),
                                it))
                        }
                        build?.let { add(equal(root.get<ProjectBuildEntity>("build"), it)) }
                        buildNumber?.let {
                            add(equal(root.get<ProjectBuildEntity>("build").get<Int>("buildNumber"),
                                it))
                        }
                        name?.let { add(equal(root.get<String>("name"), it)) }
                        hash?.let { add(equal(root.get<String>("hash"), it)) }
                        downloadUrl?.let { add(equal(root.get<String>("download_url"), it)) }
                    }.toTypedArray())
                }
            }).singleResultOrNull
        }
    }

    fun sync(project: ProjectEntity,
             version: ProjectVersionEntity,
             build: ProjectBuildEntity,
             name: String,
             hash:String?,
             createAction:(ProjectDownloadEntity)->Unit={}
    ):ProjectDownloadEntity {
        session {
            var download = getByArguments(
                project = project,
                version = version,
                build = build,
                name = name
            )
            if (download == null) {
                download = ProjectDownloadEntity()
                download.project = project
                download.version = version
                download.build = build
                download.name = name
                if (hash != null) {
                    download.hash = hash
                }
                download.status = SyncStatus.SUCCESS
                createAction(download)
                transaction { persist(download) }
            } else {
                if (hash != null && download.hash != hash) {
                    logger.info(
                        "Sync download with different hash(project={}, version={}. build={}, name={}, hash={}, newHash={})",
                        project.name, version.name, build.buildNumber, name, download.hash, hash
                    )
                }
            }
            return download
        }
    }
}