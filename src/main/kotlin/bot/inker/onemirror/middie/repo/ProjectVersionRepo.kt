package bot.inker.onemirror.middie.repo

import bot.inker.onemirror.middie.entity.ProjectEntity
import bot.inker.onemirror.middie.entity.ProjectVersionEntity
import bot.inker.onemirror.middie.entity.SyncStatus
import bot.inker.onemirror.middie.session
import bot.inker.onemirror.middie.transaction
import jakarta.persistence.criteria.Predicate

object ProjectVersionRepo {
    fun getAll(project: ProjectEntity):List<ProjectVersionEntity>{
        session {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectVersionEntity::class.java).apply {
                    val root = from(ProjectVersionEntity::class.java)
                    where(equal(root.get<ProjectEntity>("project"), project))
                }
            }).resultList
        }
    }

    fun getByArguments(
        project: ProjectEntity? = null,
        projectName: String? = null,
        name: String? = null,
    ): ProjectVersionEntity? {
        session {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectVersionEntity::class.java).apply {
                    val root = from(ProjectVersionEntity::class.java)
                    where(*ArrayList<Predicate>().apply {
                        project?.let { add(equal(root.get<ProjectEntity>("project"), it)) }
                        projectName?.let { add(equal(root.get<ProjectEntity>("project").get<String>("name"), it)) }
                        name?.let { add(equal(root.get<String>("name"), it)) }
                    }.toTypedArray())
                }
            }).singleResultOrNull
        }
    }

    private fun create(project: ProjectEntity, name: String): ProjectVersionEntity {
        transaction {
            return ProjectVersionEntity().apply {
                this.project = project
                this.name = name
            }.also {
                persist(it)
            }
        }
    }

    fun sync(project: ProjectEntity, name: String, createAction:(ProjectVersionEntity)->Unit={}): ProjectVersionEntity {
        session {
            var version = getByArguments(
                project = project,
                name = name
            )
            if(version == null){
                version = ProjectVersionEntity()
                version.project = project
                version.name = name
                version.status = SyncStatus.SUCCESS
                createAction(version)
                transaction { persist(version) }
            }
            return version
        }
    }
}