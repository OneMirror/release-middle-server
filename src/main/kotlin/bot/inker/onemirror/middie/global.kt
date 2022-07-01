package bot.inker.onemirror.middie

import bot.inker.onemirror.middie.entity.ProjectBuildEntity
import bot.inker.onemirror.middie.entity.ProjectDownloadEntity
import bot.inker.onemirror.middie.entity.ProjectEntity
import bot.inker.onemirror.middie.entity.ProjectVersionEntity
import okhttp3.OkHttpClient
import org.hibernate.Session
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import java.util.*

val okclient = OkHttpClient.Builder()
    .addInterceptor { it.proceed(
        it.request().newBuilder()
            .header(
                "user-agent",
                OneMirrorProperties["user_agent"]
            )
            .build()
    ) }
    .build()

val hibernate by lazy {
    val configuration = Configuration()
    configuration.setProperty(AvailableSettings.CONNECTION_PROVIDER, "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
    configuration.setProperty(AvailableSettings.DRIVER, OneMirrorProperties["database_driver_class"])
    configuration.setProperty(AvailableSettings.DIALECT, OneMirrorProperties["database_dialect_class"])
    configuration.setProperty(AvailableSettings.URL, OneMirrorProperties["database_url"])
    configuration.setProperty(AvailableSettings.USER, OneMirrorProperties["database_username"])
    configuration.setProperty(AvailableSettings.PASS, OneMirrorProperties["database_password"])
    configuration.setProperty(AvailableSettings.POOL_SIZE, OneMirrorProperties["database_pool_size"])
    configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "update")

    configuration.addAnnotatedClass(ProjectEntity::class.java)
    configuration.addAnnotatedClass(ProjectVersionEntity::class.java)
    configuration.addAnnotatedClass(ProjectBuildEntity::class.java)
    configuration.addAnnotatedClass(ProjectDownloadEntity::class.java)


    val serviceRegistry = StandardServiceRegistryBuilder()
        .applySettings(configuration.properties)
        .addService(
            ClassLoaderService::class.java, ClassLoaderServiceImpl(
                ArrayList<ClassLoader>().apply {
                    add(this::class.java.classLoader)
                }, TcclLookupPrecedence.AFTER
            )
        )
        .build()
    configuration.buildSessionFactory(serviceRegistry)
}

val `$threadLocalSession` = ThreadLocal<Session>()
inline fun <R> transaction(action: Session.() -> R): R {
    if (`$threadLocalSession`.get()?.isOpen == true) {
        val session = `$threadLocalSession`.get()
        val transaction = session.beginTransaction()
        try{
            return action.invoke(session)
        }finally {
            transaction.commit()
        }
    }
    hibernate.openSession().use { session ->
        `$threadLocalSession`.set(session)
        val transaction = session.beginTransaction()
        try{
            return action.invoke(session)
        }finally {
            transaction.commit()
        }
    }
}

inline fun <R> session(action: Session.() -> R): R {
    if (`$threadLocalSession`.get()?.isOpen == true) {
        val session = `$threadLocalSession`.get()
        return action.invoke(session)
    }
    hibernate.openSession().use { session ->
        `$threadLocalSession`.set(session)
        return action.invoke(session)
    }
}