package bot.inker.onemirror.middie.util

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

object DigestUtil {
    fun sha256(input:InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(4096)
        var bytes = input.read(buffer)
        while (bytes >= 0) {
            digest.update(buffer, 0, bytes)
            bytes = input.read(buffer)
        }
        return HexFormat.of().formatHex(digest.digest())
    }
    fun sha256(path:Path): String {
        return Files.newInputStream(path).use {
            sha256(it)
        }
    }
}