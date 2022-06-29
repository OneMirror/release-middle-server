/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package bot.inker.onemirror.middie.directory

import bot.inker.onemirror.middie.resource.StatusResource
import io.undertow.UndertowLogger
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.resource.Resource
import io.undertow.util.*
import org.json.JSONWriter
import org.xnio.channels.Channels
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Stuart Douglas
 */
// TODO: remove page, provide it in web-ui
class OneDirectoryUtils(
    val page: OneMirrorFileListPage,
) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)

    /**
     * Serve static resource for the directory listing
     *
     * @param exchange The exchange
     * @return true if resources were served
     */
    fun sendRequestedBlobs(exchange: HttpServerExchange): Boolean {
        var buffer: ByteBuffer? = null
        var type: String? = null
        var etag: String? = null
        var quotedEtag: String? = null
        if ("css" == exchange.queryString) {
            buffer = page.CSS_BUFFER.duplicate()
            type = "text/css"
            etag = page.FILE_CSS_ETAG
            quotedEtag = page.FILE_CSS_ETAG_QUOTED
        } else if ("js" == exchange.queryString) {
            buffer = page.JS_BUFFER.duplicate()
            type = "application/javascript"
            etag = page.FILE_JS_ETAG
            quotedEtag = page.FILE_JS_ETAG_QUOTED
        }
        if (buffer != null) {
            if (!ETagUtils.handleIfNoneMatch(exchange, ETag(false, etag), false)) {
                exchange.statusCode = StatusCodes.NOT_MODIFIED
                return true
            }
            exchange.responseHeaders.put(Headers.CONTENT_LENGTH, buffer.limit().toString())
            exchange.responseHeaders.put(Headers.CONTENT_TYPE, type)
            exchange.responseHeaders.put(Headers.ETAG, quotedEtag)
            if (Methods.HEAD.equals(exchange.requestMethod)) {
                exchange.endExchange()
                return true
            }
            exchange.responseSender.send(buffer)
            return true
        }
        return false
    }

    fun compareResource(o1: Resource, o2: Resource): Int {
        val cmp1 = java.lang.Boolean.compare(o1.isDirectory, o2.isDirectory)
        return if (cmp1 != 0) {
            -cmp1
        } else java.lang.String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name)
    }

    fun writeResource(writer: JSONWriter, resource: Resource) {
        writer.key("d").value(resource.isDirectory)
        writer.key("n").value(resource.name)
        val lastModified = resource.lastModified
        if (lastModified == null) {
            writer.key("m").value(null)
        } else {
            writer.key("m").value(lastModified.time / 1000)
        }
        val contentLength = resource.contentLength
        if (contentLength == null) {
            writer.key("s").value(null)
        } else {
            writer.key("s").value(contentLength.toLong())
        }
        val eTag = resource.eTag
        if (eTag == null) {
            writer.key("t").value(null)
        } else {
            writer.key("t").value(eTag.tag)
        }
    }

    /**
     * d: is directory
     * n: name
     * m: last modified
     * s: size
     * t: etag (sha256)
     * l: file list
     * @param exchange
     * @param resource
     * @return
     */
    fun sendRequestedJson(exchange: HttpServerExchange, resource: Resource): Boolean {
        if ("json" != exchange.queryString) {
            return false
        }
        val builder = StringBuilder()
        val writer = JSONWriter(builder)
        writer.`object`()
        writeResource(writer, resource)
        if (resource.isDirectory) {
            writer.key("l")
            writer.array()
            val resourceList = resource.list()
            resourceList.sortWith(this::compareResource)
            for (entry in resourceList) {
                writer.`object`()
                writeResource(writer, entry)
                writer.endObject()
            }
            writer.endArray()
        } else {
            writer.key("l").value(null)
        }
        writer.endObject()
        try {
            val output = ByteBuffer.wrap(builder.toString().toByteArray(StandardCharsets.UTF_8))
            exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/json; charset=UTF-8")
            exchange.responseHeaders.put(Headers.CONTENT_LENGTH, output.limit().toString())
            exchange.responseHeaders.put(Headers.LAST_MODIFIED, DateUtils.toDateString(Date()))
            exchange.responseHeaders.put(Headers.CACHE_CONTROL, "must-revalidate")
            Channels.writeBlocking(exchange.responseChannel, output)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        } catch (e: IOException) {
            UndertowLogger.REQUEST_IO_LOGGER.ioException(e)
            exchange.statusCode = StatusCodes.INTERNAL_SERVER_ERROR
        }
        exchange.endExchange()
        return true
    }

    fun renderDirectoryListing(path: String, resource: Resource): String {
        var path = path
        if (!path.endsWith("/")) {
            path += "/"
        }
        var state = 0
        var parent: String? = null
        if (path.length > 1) {
            for (i in path.length - 1 downTo 0) {
                if (state == 1) {
                    if (path[i] == '/') {
                        state = 2
                    }
                } else if (path[i] != '/') {
                    if (state == 2) {
                        parent = path.substring(0, i + 1)
                        break
                    }
                    state = 1
                }
            }
            if (parent == null) {
                parent = "/"
            }
        }
        val lists = ArrayList<List<String>>()
        if (parent != null) {
            lists.add(listOf(
                """<a class="icon up" href="$parent">[..]</a>""",
                resource.lastModified?.let(dateFormat::format) ?: "--",
                resource.contentLength?.let(this::formatSize) ?: "--",
                resource.eTag?.tag ?: "--",
                if( resource is StatusResource){ resource.status.name }else{ "--" }
            ))
        }
        val resourceList = resource.list()
        resourceList.sortWith(this::compareResource)
        for (entry in resourceList) {
            lists.add(listOf(
                """<a class="icon ${if(entry.isDirectory){"dir"}else{"file"} }" href="$path${entry.name}">${entry.name}</a>""",
                entry.lastModified?.let(dateFormat::format) ?: "--",
                entry.contentLength?.let(this::formatSize) ?: "--",
                entry.eTag?.tag ?: "--",
                if( entry is StatusResource){ entry.status.name }else{ "--" }
            ))
        }
        return page.build(path, lists)
    }

    fun renderDirectoryListing(exchange: HttpServerExchange, resource: Resource) {
        val requestPath = exchange.requestPath
        if (!requestPath.endsWith("/")) {
            exchange.statusCode = StatusCodes.FOUND
            exchange.responseHeaders.put(Headers.LOCATION,
                RedirectBuilder.redirect(exchange, exchange.relativePath + "/", true))
            exchange.endExchange()
            return
        }
        val page = renderDirectoryListing(requestPath, resource)
        try {
            val output = ByteBuffer.wrap(page.toByteArray(StandardCharsets.UTF_8))
            exchange.responseHeaders.put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8")
            exchange.responseHeaders.put(Headers.CONTENT_LENGTH, output.limit().toString())
            exchange.responseHeaders.put(Headers.LAST_MODIFIED, DateUtils.toDateString(Date()))
            exchange.responseHeaders.put(Headers.CACHE_CONTROL, "must-revalidate")
            Channels.writeBlocking(exchange.responseChannel, output)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        } catch (e: IOException) {
            UndertowLogger.REQUEST_IO_LOGGER.ioException(e)
            exchange.statusCode = StatusCodes.INTERNAL_SERVER_ERROR
        }
        exchange.endExchange()
    }

    private fun formatSize(size: Long): String {
        val builder = StringBuilder()
        var n = 1024 * 1024 * 1024
        var type = 0
        while (size < n && n >= 1024) {
            n /= 1024
            type++
        }
        var top = size * 100 / n
        var bottom = top % 100
        top /= 100
        builder.append(top)
        if (bottom > 0) {
            builder.append(".").append(bottom / 10)
            bottom %= 10
            if (bottom > 0) {
                builder.append(bottom)
            }
        }
        when (type) {
            0 -> builder.append(" GB")
            1 -> builder.append(" MB")
            2 -> builder.append(" KB")
        }
        return builder.toString()
    }
}