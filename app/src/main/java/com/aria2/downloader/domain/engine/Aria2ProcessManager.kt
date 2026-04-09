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

        val binary = installBinaryIfNeeded()
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

        repeat(30) {
            if (rpcClient.isAvailable()) {
                return@withContext
            }
            delay(300)
        }

        throw IllegalStateException(
            "aria2 daemon did not start. This bundled build currently supports arm64-v8a devices only."
        )
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        runCatching { rpcClient.shutdown() }
        process?.destroy()
        process = null
    }

    private fun installBinaryIfNeeded(): File {
        require(
            Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }
        ) { "The bundled aria2 binary only supports arm64-v8a devices." }

        val binDir = File(context.filesDir, "aria2/bin").apply { mkdirs() }
        val outFile = File(binDir, "aria2c")
        if (!outFile.exists() || outFile.length() == 0L) {
            context.assets.open("bin/aria2c-arm64-v8a").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        outFile.setExecutable(true)

        val wrapper = File(binDir, "aria2-wrapper.sh")
        if (wrapper.exists()) {
            wrapper.setExecutable(true)
        }

        return outFile
    }

    private fun installWrapper(binary: File): File {
        val wrapper = File(binary.parentFile, "aria2-wrapper.sh")
        val script = """
            |#!/system/bin/sh
            |BIN_DIR="${'$'}(CDPATH= cd -- "${'$'}(dirname -- "${'$'}0")" && pwd)"
            |DNS1="${'$'}(getprop net.dns1)"
            |DNS2="${'$'}(getprop net.dns2)"
            |if [ -d /etc/security/cacerts ]; then
            |  cat /etc/security/cacerts/* | "${'$'}BIN_DIR/aria2c" \
            |    --ca-certificate=/proc/self/fd/0 \
            |    --async-dns \
            |    --async-dns-server="${'$'}{DNS1},${'$'}{DNS2}" \
            |    "${'$'}@"
            |else
            |  "${'$'}BIN_DIR/aria2c" "${'$'}@"
            |fi
        """.trimMargin()

        if (!wrapper.exists() || wrapper.readText() != script) {
            wrapper.writeText(script)
        }
        wrapper.setExecutable(true)
        return wrapper
    }
}
