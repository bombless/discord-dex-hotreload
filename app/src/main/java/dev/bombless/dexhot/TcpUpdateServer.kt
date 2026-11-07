package dev.bombless.dexhot

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Debug-only, loopback TCP HTTP server. Expose it to the host with adb forward. */
class TcpUpdateServer(context: Context, private val manager: HotModuleManager) {
    private val appContext = context.applicationContext
    @Volatile private var running = false
    private var server: ServerSocket? = null
    private var thread: Thread? = null

    @Synchronized fun start() {
        if (running) return
        check(BuildConfig.DEBUG) { "Hot reload server is disabled in non-debuggable builds" }
        running = true
        thread = Thread({
            try {
                ServerSocket(PORT, 16, InetAddress.getByName("127.0.0.1")).use { socket ->
                    server = socket
                    while (running) {
                        val client = socket.accept()
                        Thread({ handle(client) }, "dex-upload-client").start()
                    }
                }
            } catch (t: Throwable) {
                if (running) Log.e(TAG, "TCP server stopped", t)
            } finally { running = false }
        }, "dex-hot-reload-server").apply { isDaemon = true; start() }
    }

    @Synchronized fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            try {
                val requestLine = readLine(input) ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return respond(output, 400, "Bad request")
                val method = parts[0]
                val path = parts[1]
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readLine(input) ?: break
                    if (line.isEmpty()) break
                    val index = line.indexOf(':')
                    if (index > 0) headers[line.substring(0, index).trim().lowercase()] = line.substring(index + 1).trim()
                }
                when {
                    method == "GET" && path == "/health" -> respond(output, 200, "ok")
                    method == "GET" && path == "/versions" -> respond(output, 200, manager.versions().joinToString("\n"))
                    method == "POST" && path.startsWith("/modules/") -> {
                        val version = URLDecoder.decode(path.removePrefix("/modules/").substringBefore("?"), "UTF-8")
                        val query = path.substringAfter("?", "")
                        val entry = query.split("&").firstOrNull { it.startsWith("entry=") }?.substringAfter("=")?.let { URLDecoder.decode(it, "UTF-8") }
                            ?: headers["x-entry-class"] ?: error("Missing entry class")
                        val length = headers["content-length"]?.toIntOrNull() ?: error("Content-Length required")
                        require(length in 1..HotModuleManager.MAX_DEX_BYTES) { "Invalid Content-Length" }
                        val bytes = ByteArray(length)
                        var offset = 0
                        while (offset < length) {
                            val count = input.read(bytes, offset, length - offset)
                            if (count < 0) error("Unexpected end of upload")
                            offset += count
                        }
                        val loaded = manager.install(version, bytes, entry)
                        respond(output, 200, "activated ${loaded.version}")
                    }
                    method == "POST" && path.startsWith("/rollback/") -> {
                        val version = URLDecoder.decode(path.removePrefix("/rollback/"), "UTF-8")
                        val loaded = manager.activate(version)
                        respond(output, 200, "activated ${loaded.version}")
                    }
                    method == "POST" && path == "/rollback" -> respond(output, 200, "activated ${manager.rollback().version}")
                    else -> respond(output, 404, "not found")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Request failed", t)
                respond(output, 400, t.message ?: "request failed")
            }
        }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        while (bytes.size < 8192) {
            val value = input.read()
            if (value < 0) return if (bytes.isEmpty()) null else String(bytes.toByteArray(), StandardCharsets.UTF_8)
            if (value == 10) break
            if (value != 13) bytes.add(value.toByte())
        }
        return String(bytes.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun respond(output: BufferedOutputStream, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val label = if (status == 200) "OK" else if (status == 404) "Not Found" else "Bad Request"
        output.write("HTTP/1.1 $status $label\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        output.write(bytes)
        output.flush()
    }

    companion object { const val PORT = 47653; private const val TAG = "DexHotReload" }
}
