package bot.inker.onemirror.middie.util

import bot.inker.onemirror.middie.resource.Resource
import com.google.gson.stream.JsonWriter

object RestResourceApi {
    fun list(writer: JsonWriter, resource: Resource, depth:Int = 0){
        writer.beginObject()
        writer.name("isDirectory").value(resource.isDirectory)
        writer.name("name").value(resource.name)
        writer.name("lastModified").value(resource.lastModified)
        writer.name("size").value(resource.size)
        writer.name("etag").value(resource.etag)
        writer.name("list")
        if(depth > 0 && resource.isDirectory){
            writer.beginArray()
            resource.fileList
                .filter(Resource::isDirectory)
                .forEach { list(writer, it, depth - 1) }
            writer.endArray()
        }else{
            writer.nullValue()
        }
        writer.endObject()
    }
}