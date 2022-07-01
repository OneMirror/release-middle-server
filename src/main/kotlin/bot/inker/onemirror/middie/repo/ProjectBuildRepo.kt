package bot.inker.onemirror.middie.repo

import bot.inker.onemirror.middie.entity.ProjectBuildEntity
import bot.inker.onemirror.middie.entity.ProjectEntity
import bot.inker.onemirror.middie.entity.ProjectVersionEntity
import bot.inker.onemirror.middie.entity.SyncStatus
import bot.inker.onemirror.middie.session
import bot.inker.onemirror.middie.transaction
import jakarta.persistence.criteria.Predicate

object ProjectBuildRepo {
    fun getByArguments(
        project: ProjectEntity? = null,
        projectName: String? = null,
        version: ProjectVersionEntity? = null,
        versionName: String? = null,
        buildNumber: Int? = null,
        change: String? = null,
    ): ProjectBuildEntity? {
        session {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectBuildEntity::class.java).apply {
                    val root = from(ProjectBuildEntity::class.java)
                    where(*ArrayList<Predicate>().apply {
                        project?.let { add(equal(root.get<ProjectEntity>("project"), it)) }
                        projectName?.let { add(equal(root.get<ProjectEntity>("project").get<String>("name"), it)) }
                        version?.let { add(equal(root.get<ProjectVersionEntity>("version"), it)) }
                        projectName?.let {
                            add(equal(root.get<ProjectVersionEntity>("version").get<String>("name"),
                                it))
                        }
                        buildNumber?.let { add(equal(root.get<Int>("buildNumber"), it)) }
                        change?.let { add(equal(root.get<String>("change"), it)) }
                    }.toTypedArray())
                }
            }).singleResultOrNull
        }
    }


    private fun create(
        project: ProjectEntity,
        version: ProjectVersionEntity,
        buildNumber: Int,
        change: String,
    ): ProjectBuildEntity {
        transaction {
            return ProjectBuildEntity().apply {
                this.project = project
                this.version = version
                this.buildNumber = buildNumber
                this.change = change
            }.also {
                persist(it)
            }
        }
    }

    fun getAll(version: ProjectVersionEntity): List<ProjectBuildEntity> {
        session {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectBuildEntity::class.java).apply {
                    val root = from(ProjectBuildEntity::class.java)
                    where(equal(root.get<ProjectVersionEntity>("version"), version))
                }
            }).resultList
        }
    }

    fun getNextBuildNumber():Int {
        return session {
            createQuery(criteriaBuilder.run {
                createQuery(ProjectBuildEntity::class.java).apply {
                    val root = from(ProjectBuildEntity::class.java)
                    select(root.get("buildNumber"))
                    orderBy(desc(root.get<Int>("buildNumber")))
                }
            }).apply {
                maxResults = 1
            }.singleResult
        }.buildNumber + 1
    }

    fun sync(project: ProjectEntity,
             version: ProjectVersionEntity,
             buildNumber: Int,
             change: String,
             createAction:(ProjectEntity)->Unit={}
    ): ProjectBuildEntity {
        var build = getByArguments(
            project = project,
            version = version,
            buildNumber = buildNumber
        )
        if(build == null){
            build = ProjectBuildEntity()
            build.project = project
            build.version = version
            build.buildNumber = buildNumber
            build.change = change
            build.status = SyncStatus.SUCCESS
            transaction { persist(build) }
        }
        return build
    }
}