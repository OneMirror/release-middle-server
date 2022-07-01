package bot.inker.onemirror.middie.repo

import bot.inker.onemirror.middie.entity.ProjectEntity
import bot.inker.onemirror.middie.entity.SyncStatus
import bot.inker.onemirror.middie.session
import bot.inker.onemirror.middie.transaction
import jakarta.persistence.criteria.Predicate

object ProjectRepo {
    fun getByArguments(
        name: String? = null,
    ): ProjectEntity? {
        session {
            return createQuery(criteriaBuilder.run {
                createQuery(ProjectEntity::class.java).apply {
                    val root = from(ProjectEntity::class.java)
                    where(*ArrayList<Predicate>().apply {
                        name?.let { add(equal(root.get<String>("name"), it)) }
                    }.toTypedArray())
                }
            }).singleResultOrNull
        }
    }

    private fun create(name: String): ProjectEntity {
        transaction {
            return ProjectEntity().apply {
                this.name = name
            }.also {
                persist(it)
            }
        }
    }

    fun getAllProjects(): List<ProjectEntity> {
        session {
            return createQuery(
                criteriaBuilder.createQuery(ProjectEntity::class.java).apply {
                    from(ProjectEntity::class.java)
                }
            ).resultList
        }
    }

    fun sync(name:String,createAction:(ProjectEntity)->Unit={}):ProjectEntity {
        session {
            var project = getByArguments(name)
            if(project == null){
                project = ProjectEntity()
                project.name = name
                project.status = SyncStatus.SUCCESS
                createAction(project)
                transaction { persist(project) }
            }
            return project
        }
    }
}