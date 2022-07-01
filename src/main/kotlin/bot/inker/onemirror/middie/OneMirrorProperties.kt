package bot.inker.onemirror.middie

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.LinkedHashMap

object OneMirrorProperties {
    private const val APP_NAME = "middle"
    private val properties = run {
        val properties = LinkedHashMap<String, Property>()
        properties.putAll(OneMirrorProperties::class.java.getResourceAsStream("default.properties")!!.use {
            Properties().apply{ load(InputStreamReader(it, StandardCharsets.UTF_8)) }
        }.map { it.key.toString().lowercase() to Property(it.value.toString(), "default") })

        val propertiesFile = Paths.get("$APP_NAME.properties")
        if(Files.exists(propertiesFile)){
            properties.putAll(
                Files.newBufferedReader(propertiesFile).use {
                    Properties().apply{ load(it) }
                }.map { it.key.toString().lowercase() to Property(it.value.toString(), "file") }
            )
        }

        properties.putAll(
            System.getProperties().map {
                it.key.toString().lowercase() to it.value
            }.filter {
                it.first.startsWith("$APP_NAME.")
            }.map {
                it.first.substring("$APP_NAME.".length) to Property(it.second.toString(), "system")
            }
        )

        properties.putAll(
            System.getenv().map {
                it.key.lowercase() to it.value
            }.filter {
                it.first.startsWith("${APP_NAME}_")
            }.map {
                it.first.substring("${APP_NAME}_".length) to Property(it.second.toString(), "env")
            }
        )
        properties.toMap()
    }
    operator fun get(key:String) =
        requireNotNull(properties.get(key)){
            "No properties $key found"
        }.value
    operator fun invoke() = properties
    data class Property(
        val value:String,
        val source:String
    )
}