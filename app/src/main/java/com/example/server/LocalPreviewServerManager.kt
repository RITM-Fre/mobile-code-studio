package com.example.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface ServerStatus {
    data object Stopped : ServerStatus
    data class Running(
        val port: Int,
        val projectId: String,
        val baseUrl: String
    ) : ServerStatus
    data class Error(val message: String, val port: Int) : ServerStatus
}

class LocalPreviewServerManager {
    private val _status = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private var engine: LocalHttpServerEngine? = null
    private var activePort: Int = -1

    suspend fun startForProject(
        projectId: String,
        projectRoot: File,
        requestedPort: Int
    ): Result<ServerStatus.Running> = withContext(Dispatchers.IO) {
        try {
            val current = engine
            if (current != null && current.isRunning && activePort == requestedPort) {
                current.setRootDirectory(projectRoot)
                val runningState = ServerStatus.Running(
                    port = requestedPort,
                    projectId = projectId,
                    baseUrl = "http://127.0.0.1:$requestedPort"
                )
                _status.value = runningState
                return@withContext Result.success(runningState)
            }

            current?.stop()
            val newEngine = LocalHttpServerEngine(projectRoot, requestedPort)
            newEngine.start()
            engine = newEngine
            activePort = requestedPort

            val runningState = ServerStatus.Running(
                port = requestedPort,
                projectId = projectId,
                baseUrl = "http://127.0.0.1:$requestedPort"
            )
            _status.value = runningState
            Result.success(runningState)
        } catch (t: Throwable) {
            val message = t.localizedMessage ?: "Failed to bind port $requestedPort"
            _status.value = ServerStatus.Error(message = message, port = requestedPort)
            Result.failure(t)
        }
    }

    fun buildPreviewUrl(baseUrl: String, entryRelativePath: String): String {
        val clean = entryRelativePath.trimStart('/')
        if (clean.isEmpty()) return "$baseUrl/index.html"
        val encodedSegments = clean.split('/').joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")
        }
        return "$baseUrl/$encodedSegments"
    }

    fun stopServer() {
        engine?.stop()
        engine = null
        activePort = -1
        _status.value = ServerStatus.Stopped
    }
}
