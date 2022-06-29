package bot.inker.onemirror.middie

import bot.inker.onemirror.middie.util.DigestUtil
import java.io.InputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object BlobManager {
    fun createBlob(path: Path,hash:String?):String{
        val hash = hash?:DigestUtil.sha256(path)
        val blobPath = Path.of("blob", hash.substring(0,2), hash)
        if(!Files.exists(blobPath)){
            Files.createDirectories(blobPath.parent)
            Files.move(path, blobPath)
        }
        return hash
    }
    fun getBlob(hash:String?): Blob? {
        hash?:return null
        val blobPath = Path.of("blob", hash.substring(0,2), hash)
        if (Files.exists(blobPath)) {
            return Blob.FileSystem(blobPath)
        }
        return null
    }

    sealed interface Blob{
        val contentLength:Long
        fun stream():InputStream
        interface WithRandomAccess:Blob{
            fun channel():FileChannel
        }
        class FileSystem(
            private val blobPath:Path
        ): WithRandomAccess {
            override val contentLength = Files.size(blobPath)
            override fun stream() = Files.newInputStream(blobPath, StandardOpenOption.READ)
            override fun channel() = FileChannel.open(blobPath, StandardOpenOption.READ)
        }
    }
}