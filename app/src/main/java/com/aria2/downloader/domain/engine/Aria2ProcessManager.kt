package com.aria2.downloader.domain.engine

import android.content.Context
import android.os.Build
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class Aria2ProcessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val rpcClient: Aria2RpcClient
) {
    @Volatile
    private var process: Process? = null

    private val processMutex = Mutex()

    private val stagingDirectory: File
        get() = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
            "Aria2Downloads"
        ).apply { mkdirs() }

    fun resolveDownloadDirectory(settings: AppSettings): File {
        return if (settings.downloadLocationUri == null) {
            File(settings.downloadLocationPath).apply { mkdirs() }
        } else {
            stagingDirectory
        }
    }

    suspend fun ensureRunning() = withContext(Dispatchers.IO) {
        processMutex.withLock {
            if (rpcClient.isAvailable()) {
                return@withLock
            }

            val settings = settingsRepository.currentSettings()
            val targetDownloadDir = resolveDownloadDirectory(settings)
            if (!targetDownloadDir.exists()) targetDownloadDir.mkdirs()
            require(targetDownloadDir.exists()) {
                "Could not create download directory: ${targetDownloadDir.absolutePath}"
            }

            val reusablePort = findReusableRpcPort()
            if (reusablePort != null) {
                Aria2Runtime.rpcPort = reusablePort
                return@withLock
            }

            if (process?.isAlive == true) {
                repeat(12) {
                    if (rpcClient.isAvailable()) return@withLock
                    delay(250)
                }
                if (rpcClient.isAvailable()) return@withLock
                process?.destroy()
                process = null
                delay(300)
            }

            val binary = resolveBundledBinary()
            val wrapper = installWrapper(binary)
            val homeDir = File(context.filesDir, "aria2/home").apply { mkdirs() }
            val sessionFile = File(homeDir, "session.txt").apply { if (!exists()) createNewFile() }
            val logFile = File(homeDir, "aria2.log").apply { if (!exists()) createNewFile() }

            var lastErrorText = ""
            var lastExitCode: Int? = null

            repeat(2) { attemptIndex ->
                val preexistingPort = findReusableRpcPort()
                if (preexistingPort != null) {
                    Aria2Runtime.rpcPort = preexistingPort
                    return@withLock
                }

                val chosenPort = firstBindablePort() ?: Aria2Runtime.DEFAULT_RPC_PORT
                Aria2Runtime.rpcPort = chosenPort
                logFile.appendText("\n---- aria2 startup attempt ${attemptIndex + 1} on port $chosenPort ----\n")

                val aria2Args = mutableListOf(
                    "--enable-rpc=true",
                    "--rpc-listen-all=false",
                    "--rpc-listen-port=$chosenPort",
                    "--rpc-secret=${settings.rpcToken}",
                    "--dir=${targetDownloadDir.absolutePath}",
                    "--continue=true",
                    "--auto-file-renaming=true",
                    "--allow-overwrite=false",
                    "--max-concurrent-downloads=${settings.maxConcurrentDownloads}",
                    "--split=${settings.split}",
                    "--max-connection-per-server=${settings.maxConnectionPerServer}",
                    "--min-split-size=${settings.minSplitSizeMb}M",
                    "--file-allocation=none",
                    "--summary-interval=0",
                    "--disk-cache=16M",
                    "--enable-dht=${settings.enableDht}",
                    "--enable-peer-exchange=${settings.peerExchange}",
                    "--bt-enable-lpd=${settings.localPeerDiscovery}",
                    "--bt-require-crypto=${settings.requireEncryption}",
                    "--follow-torrent=true",
                    "--follow-metalink=true",
                    "--bt-save-metadata=true",
                    "--rpc-save-upload-metadata=true",
                    "--save-session=${sessionFile.absolutePath}",
                    "--input-file=${sessionFile.absolutePath}",
                    "--save-session-interval=30",
                    "--seed-time=0",
                    "--log=${logFile.absolutePath}",
                    "--stop-with-process=${android.os.Process.myPid()}"
                )

                val command = mutableListOf("/system/bin/sh", wrapper.absolutePath).apply {
                    addAll(aria2Args)
                }

                process = ProcessBuilder(command)
                    .directory(homeDir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                    .start()

                for (poll in 0 until 60) {
                    if (rpcClient.isAvailableOnPort(chosenPort)) {
                        Aria2Runtime.rpcPort = chosenPort
                        Aria2Runtime.lastStartupError = ""
                        Aria2Runtime.lastStartupLog = readLogTail(logFile)
                        return@withLock
                    }

                    val exited = runCatching { process?.exitValue() }.getOrNull()
                    if (exited != null) {
                        break
                    }
                    delay(250)
                }

                val latePort = findReusableRpcPort()
                if (latePort != null) {
                    Aria2Runtime.rpcPort = latePort
                    Aria2Runtime.lastStartupError = ""
                    Aria2Runtime.lastStartupLog = readLogTail(logFile)
                    return@withLock
                }

                lastExitCode = runCatching { process?.exitValue() }.getOrNull()
                lastErrorText = readLogTail(logFile)
                Aria2Runtime.lastStartupLog = lastErrorText
                Aria2Runtime.lastStartupError = classifyStartupFailure(lastErrorText, lastExitCode)

                if (lastErrorText.contains("Address already in use", ignoreCase = true) ||
                    lastErrorText.contains("failed to bind TCP port", ignoreCase = true)) {
                    val reused = findReusableRpcPort()
                    if (reused != null) {
                        Aria2Runtime.rpcPort = reused
                        return@withLock
                    }
                }

                process?.destroy()
                process = null
                delay(300)
            }

            throw IllegalStateException(
                buildString {
                    append(Aria2Runtime.lastStartupError.ifBlank { "aria2 daemon did not start" })
                    lastExitCode?.let { append(" (exit code ").append(it).append(')') }
                    if (lastErrorText.isNotBlank()) {
                        append(". Log: ")
                        append(lastErrorText)
                    }
                }
            )
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        processMutex.withLock {
            runCatching { rpcClient.shutdown() }
            process?.destroy()
            process = null
        }
    }

    private suspend fun findReusableRpcPort(): Int? {
        for (port in Aria2Runtime.RPC_PORT_RANGE) {
            if (rpcClient.isAvailableOnPort(port)) return port
        }
        return null
    }

    private fun firstBindablePort(): Int? = Aria2Runtime.RPC_PORT_RANGE.firstOrNull(::canBind)

    private fun canBind(port: Int): Boolean = runCatching {
        ServerSocket(port).use { socket -> socket.reuseAddress = true }
        true
    }.getOrDefault(false)

    private fun resolveBundledBinary(): File {
        require(Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }) {
            "This build currently supports arm64-v8a only."
        }

        val nativeBinary = File(context.applicationInfo.nativeLibraryDir, "libaria2c.so")
        if (nativeBinary.exists()) {
            nativeBinary.setExecutable(true)
            return nativeBinary
        }

        val binDir = File(context.filesDir, "aria2/bin").apply { mkdirs() }
        val outFile = File(binDir, "aria2c")
        if (!outFile.exists() || outFile.length() == 0L) {
            context.assets.open("bin/aria2c-arm64-v8a").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        outFile.setExecutable(true)
        return outFile
    }

    private fun installWrapper(binary: File): File {
        val wrapper = File(context.filesDir, "aria2/bin/aria2-wrapper.sh")
        wrapper.parentFile?.mkdirs()

        val script = """
            |#!/system/bin/sh
            |ARIA2_BIN="${binary.absolutePath}"
            |DNS1="${'$'}(getprop net.dns1)"
            |DNS2="${'$'}(getprop net.dns2)"
            |if [ -d /etc/security/cacerts ]; then
            |  cat /etc/security/cacerts/* | "${'$'}ARIA2_BIN" \
            |    --ca-certificate=/proc/self/fd/0 \
            |    --async-dns \
            |    --async-dns-server="${'$'}{DNS1},${'$'}{DNS2}" \
            |    "${'$'}@"
            |else
            |  "${'$'}ARIA2_BIN" "${'$'}@"
            |fi
        """.trimMargin()

        if (!wrapper.exists() || wrapper.readText() != script) {
            wrapper.writeText(script)
        }
        wrapper.setReadable(true, true)
        wrapper.setWritable(true, true)
        wrapper.setExecutable(true, true)
        return wrapper
    }

    private fun readLogTail(logFile: File): String = runCatching {
        logFile.takeIf { it.exists() }?.readText()?.takeLast(5000)?.trim().orEmpty()
    }.getOrDefault("")

    private fun classifyStartupFailure(logTail: String, exitCode: Int?): String {
        val lower = logTail.lowercase()
        return when {
            "permission denied" in lower || "operation not permitted" in lower ->
                "aria2 startup failed because Android denied executing the daemon or writing to the target directory"
            "failed to bind tcp port" in lower || "address already in use" in lower ->
                "aria2 RPC port conflict detected"
            "cannot open existing file" in lower || "could not create new file" in lower || "could not create directory" in lower ->
                "aria2 startup failed because the download or session path is not writable"
            "unrecognized option" in lower || "bad option" in lower ->
                "aria2 startup failed because one or more command-line options are invalid"
            "ipv6 rpc: listening on tcp port" in lower && "serialized session successfully" in lower ->
                "aria2 started its RPC server but exited before the app could attach; this usually means a premature shutdown or a second startup attempt interrupted it"
            exitCode == 1 -> "aria2 exited with an unknown startup error"
            else -> "aria2 daemon did not start"
        }
    }
}
