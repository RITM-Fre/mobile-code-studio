package com.example

import com.example.server.LocalHttpServerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.nio.file.Files

class LocalHttpServerEngineTest {

    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun servesIndexAndNestedAssetsAndBlocksTraversal() {
        val tempRoot = Files.createTempDirectory("mcs_server_test").toFile()
        try {
            File(tempRoot, "index.html").writeText("<h1>Hello Mobile Code Studio</h1>")
            File(tempRoot, "css").mkdirs()
            File(tempRoot, "css/style.css").writeText("body { color: #38bdf8; }")

            val port = findFreePort()
            val engine = LocalHttpServerEngine(tempRoot, port)
            engine.start()
            try {
                // Root serves index.html
                val rootConn = URL("http://127.0.0.1:$port/").openConnection() as HttpURLConnection
                assertEquals(200, rootConn.responseCode)
                assertTrue(rootConn.contentType.orEmpty().contains("text/html"))
                val htmlBody = rootConn.inputStream.bufferedReader().readText()
                assertTrue(htmlBody.contains("Hello Mobile Code Studio"))
                rootConn.disconnect()

                // Nested CSS asset
                val cssConn = URL("http://127.0.0.1:$port/css/style.css").openConnection() as HttpURLConnection
                assertEquals(200, cssConn.responseCode)
                assertTrue(cssConn.contentType.orEmpty().contains("text/css"))
                val cssBody = cssConn.inputStream.bufferedReader().readText()
                assertTrue(cssBody.contains("#38bdf8"))
                cssConn.disconnect()

                // Missing asset returns 404
                val notFoundConn = URL("http://127.0.0.1:$port/missing.js").openConnection() as HttpURLConnection
                assertEquals(404, notFoundConn.responseCode)
                notFoundConn.disconnect()
            } finally {
                engine.stop()
            }
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
