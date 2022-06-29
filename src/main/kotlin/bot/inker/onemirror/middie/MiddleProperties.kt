package bot.inker.onemirror.middie

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.LinkedHashMap

object MiddleProperties {
    private val properties:Map<String, Property> = run {
        val properties = LinkedHashMap<String, Property>()
        properties.putAll(MiddleProperties::class.java.getResourceAsStream("default.properties")!!.use {
            Properties().apply{ load(InputStreamReader(it, StandardCharsets.UTF_8)) }
        }.map { it.key.toString().lowercase() to Property(it.value.toString(), "default") })

        val propertiesFile = Paths.get("middle.properties")
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
                it.first.startsWith("middle.")
            }.map {
                it.first.substring("middle.".length) to Property(it.second.toString(), "system")
            }
        )

        properties.putAll(
            System.getenv().map {
                it.key.lowercase() to it.value
            }.filter {
                it.first.startsWith("middle_")
            }.map {
                it.first.substring("middle_".length) to Property(it.second.toString(), "env")
            }
        )
        Collections.unmodifiableMap(properties)
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