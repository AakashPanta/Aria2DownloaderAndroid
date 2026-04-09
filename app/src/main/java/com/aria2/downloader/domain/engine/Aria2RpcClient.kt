package com.aria2.downloader.domain.engine

import android.util.Base64
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.domain.model.DownloadSourceType
import com.aria2.downloader.domain.model.DownloadStatus
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class Aria2RpcClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: SettingsRepository
) {
    private val endpoint = "http://127.0.0.1:6800/jsonrpc"

    suspend fun isAvailable(): Boolean = runCatching { getVersion(); true }.getOrDefault(false)

    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        val response = call("aria2.getVersion", emptyList())
        response.getString("version")
    }

    suspend fun addUri(uri: String, options: Map<String, String>): String = withContext(Dispatchers.IO) {
        call("aria2.addUri", listOf(listOf(uri), options)).getString("result")
    }

    suspend fun addTorrent(fileBytes: ByteArray, options: Map<String, String>): String = withContext(Dispatchers.IO) {
        val encoded = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
        call("aria2.addTorrent", listOf(encoded, emptyList<String>(), options)).getString("result")
    }

    suspend fun addMetalink(fileBytes: ByteArray, options: Map<String, String>): List<String> = withContext(Dispatchers.IO) {
        val encoded = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
        val result = call("aria2.addMetalink", listOf(encoded, options)).getJSONArray("result")
        (0 until result.length()).map { result.getString(it) }
    }

    suspend fun pause(gid: String) {
        call("aria2.pause", listOf(gid))
    }

    suspend fun unpause(gid: String) {
        call("aria2.unpause", listOf(gid))
    }

    suspend fun forceRemove(gid: String) {
        call("aria2.forceRemove", listOf(gid))
    }

    suspend fun changeOption(gid: String, options: Map<String, String>) {
        call("aria2.changeOption", listOf(gid, options))
    }

    suspend fun changeGlobalOption(options: Map<String, String>) {
        call("aria2.changeGlobalOption", listOf(options))
    }

    suspend fun changePosition(gid: String, pos: Int, how: String): Int = withContext(Dispatchers.IO) {
        call("aria2.changePosition", listOf(gid, pos, how)).getInt("result")
    }

    suspend fun tellStatus(gid: String): Aria2Task = withContext(Dispatchers.IO) {
        parseTask(call("aria2.tellStatus", listOf(gid, STATUS_KEYS)).getJSONObject("result"))
    }

    suspend fun tellActive(): List<Aria2Task> = withContext(Dispatchers.IO) {
        val result = call("aria2.tellActive", listOf(STATUS_KEYS)).getJSONArray("result")
        parseTasks(result)
    }

    suspend fun tellWaiting(offset: Int = 0, num: Int = 100): List<Aria2Task> = withContext(Dispatchers.IO) {
        val result = call("aria2.tellWaiting", listOf(offset, num, STATUS_KEYS)).getJSONArray("result")
        parseTasks(result)
    }

    suspend fun tellStopped(offset: Int = 0, num: Int = 100): List<Aria2Task> = withContext(Dispatchers.IO) {
        val result = call("aria2.tellStopped", listOf(offset, num, STATUS_KEYS)).getJSONArray("result")
        parseTasks(result)
    }

    suspend fun shutdown() {
        runCatching { call("aria2.shutdown", emptyList()) }
    }

    private suspend fun call(method: String, params: List<Any?>): JSONObject = withContext(Dispatchers.IO) {
        val settings = settingsRepository.currentSettings()
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", System.currentTimeMillis().toString())
            put("method", method)
            put("params", buildParams(settings.rpcToken, params))
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("aria2 RPC HTTP ${response.code}: $body")
            }
            val json = JSONObject(body)
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                throw IllegalStateException(error.optString("message", "Unknown aria2 RPC error"))
            }
            json
        }
    }

    private fun buildParams(token: String, params: List<Any?>): JSONArray {
        val array = JSONArray()
        array.put("token:$token")
        params.forEach { array.put(toJsonValue(it)) }
        return array
    }

    private fun toJsonValue(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply { value.forEach { (k, v) -> put(k.toString(), toJsonValue(v)) } }
        is List<*> -> JSONArray().apply { value.forEach { put(toJsonValue(it)) } }
        is Array<*> -> JSONArray().apply { value.forEach { put(toJsonValue(it)) } }
        else -> value
    }

    private fun parseTasks(array: JSONArray): List<Aria2Task> = (0 until array.length()).map {
        parseTask(array.getJSONObject(it))
    }

    private fun parseTask(json: JSONObject): Aria2Task {
        val files = json.optJSONArray("files")
        val firstFile = files?.optJSONObject(0)
        val path = firstFile?.optString("path").orEmpty()
        val firstUri = firstFile?.optJSONArray("uris")?.optJSONObject(0)?.optString("uri").orEmpty()
        val bittorrent = json.optJSONObject("bittorrent")
        val info = bittorrent?.optJSONObject("info")
        val name = when {
            path.isNotBlank() -> File(path).name
            info?.optString("name").orEmpty().isNotBlank() -> info?.optString("name").orEmpty()
            firstUri.startsWith("magnet:?") -> firstUri.substringAfter("dn=").substringBefore("&").ifBlank { "Torrent metadata" }
            else -> "download-${json.optString("gid")}" 
        }

        val sourceType = when {
            firstUri.startsWith("magnet:?") -> DownloadSourceType.MAGNET
            firstUri.endsWith(".torrent", ignoreCase = true) || bittorrent != null -> DownloadSourceType.TORRENT
            firstUri.endsWith(".meta4", ignoreCase = true) || firstUri.endsWith(".metalink", ignoreCase = true) -> DownloadSourceType.METALINK
            else -> DownloadSourceType.DIRECT
        }

        val status = when (json.optString("status")) {
            "active" -> if (json.optString("followedBy").isNotBlank()) DownloadStatus.METADATA else DownloadStatus.DOWNLOADING
            "waiting" -> DownloadStatus.QUEUED
            "paused" -> DownloadStatus.PAUSED
            "complete" -> DownloadStatus.COMPLETED
            "error" -> DownloadStatus.FAILED
            "removed" -> DownloadStatus.CANCELLED
            else -> DownloadStatus.QUEUED
        }

        return Aria2Task(
            gid = json.optString("gid"),
            source = firstUri.ifBlank { path },
            fileName = name,
            sourceType = sourceType,
            savePath = path,
            totalLength = json.optString("totalLength").toLongOrNull() ?: 0L,
            completedLength = json.optString("completedLength").toLongOrNull() ?: 0L,
            downloadSpeed = json.optString("downloadSpeed").toLongOrNull() ?: 0L,
            uploadSpeed = json.optString("uploadSpeed").toLongOrNull() ?: 0L,
            connections = json.optString("connections").toIntOrNull() ?: 0,
            status = status,
            errorCode = json.optString("errorCode").takeIf { it.isNotBlank() },
            errorMessage = json.optString("errorMessage").takeIf { it.isNotBlank() },
            selectedFiles = json.optString("selectedFiles").takeIf { it.isNotBlank() },
            infoHash = json.optString("infoHash").takeIf { it.isNotBlank() },
            following = json.optString("followedBy").takeIf { it.isNotBlank() }
        )
    }

    data class Aria2Task(
        val gid: String,
        val source: String,
        val fileName: String,
        val sourceType: DownloadSourceType,
        val savePath: String,
        val totalLength: Long,
        val completedLength: Long,
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val connections: Int,
        val status: DownloadStatus,
        val errorCode: String?,
        val errorMessage: String?,
        val selectedFiles: String?,
        val infoHash: String?,
        val following: String?
    )

    companion object {
        private val STATUS_KEYS = listOf(
            "gid",
            "status",
            "totalLength",
            "completedLength",
            "downloadSpeed",
            "uploadSpeed",
            "connections",
            "files",
            "bittorrent",
            "errorCode",
            "errorMessage",
            "selectedFiles",
            "infoHash",
            "followedBy"
        )
    }
}
