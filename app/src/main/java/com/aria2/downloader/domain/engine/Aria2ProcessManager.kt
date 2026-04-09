package com.aria2.downloader.domain.engine

import android.content.Context
import android.os.Build
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Singleton
class Aria2ProcessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val rpcClient: Aria2RpcClient
) {
    @Volatile
    private var process: Process? = null

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
        if (rpcClient.isAvailable()) {
            return@withContext
        }

        val binary = resolveBundledBinary()
        val wrapper = installWrapper(binary)
        val settings = settingsRepository.currentSettings()
        val targetDownloadDir = resolveDownloadDirectory(settings)

        if (process?.isAlive == true) {
            process?.destroy()
            process = null
        }

        val homeDir = File(context.filesDir, "aria2/home").apply { mkdirs() }
        val sessionFile = File(homeDir, "session.txt").apply { if (!exists()) createNewFile() }
        val logFile = File(homeDir, "aria2.log")
        if (!logFile.exists()) {
            logFile.createNewFile()
        }

        val aria2Args = mutableListOf(
            "--enable-rpc=true",
            "--rpc-listen-all=false",
            "--rpc-listen-port=6800",
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
            "--log=${logFile.absolutePath}"
        )

        val command = mutableListOf("/system/bin/sh", wrapper.absolutePath).apply {
            addAll(aria2Args)
        }

        process = ProcessBuilder(command)
            .directory(homeDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
            .start()

        repeat(40) {
            if (rpcClient.isAvailable()) {
                return@withContext
            }

            val exited = runCatching { process?.exitValue() }.getOrNull()
            if (exited != null) {
                break
            }

            delay(250)
        }

        val exitCode = runCatching { process?.exitValue() }.getOrNull()
        val logTail = runCatching {
            logFile.takeIf { it.exists() }
                ?.readText()
                ?.takeLast(4000)
                ?.trim()
        }.getOrNull()

        throw IllegalStateException(
            buildString {
                append("aria2 daemon did not start")
                exitCode?.let { append(" (exit code ").append(it).append(")") }
                if (!logTail.isNullOrBlank()) {
                    append(". Log: ")
                    append(logTail)
                } else {
                    append(". Check that libaria2c.so exists in app/src/main/jniLibs/arm64-v8a/")
                }
            }
        )
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        runCatching { rpcClient.shutdown() }
        process?.destroy()
        process = null
    }

    private fun resolveBundledBinary(): File {
        require(
            Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }
        ) { "This build currently supports arm64-v8a only." }

        val libDir = File(context.applicationInfo.nativeLibraryDir)
        val binary = File(libDir, "libaria2c.so")

        require(binary.exists()) {
            "Bundled aria2 binary not found at ${binary.absolutePath}"
        }

        binary.setExecutable(true)
        return binary
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
        return wrapper
    }
}
