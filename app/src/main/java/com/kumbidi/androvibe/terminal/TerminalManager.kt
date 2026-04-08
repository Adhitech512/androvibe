package com.kumbidi.androvibe.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val output: String,
    val exitCode: Int
)

class TerminalManager(private val context: Context) {

    private val rootfsDir: File by lazy {
        context.getDir("rootfs", Context.MODE_PRIVATE)
    }

    private val binDir: File by lazy {
        context.getDir("bin", Context.MODE_PRIVATE)
    }

    private val prootBinary: File by lazy {
        File(binDir, "proot")
    }

    fun isBootstrapped(): Boolean {
        return File(rootfsDir, "bin").exists() && prootBinary.exists()
    }

    // ── Execute a command in the app sandbox shell ───────────────
    suspend fun executeCommand(
        command: String,
        workingDir: File? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        try {
            val pb = ProcessBuilder("sh", "-c", command)
            if (workingDir != null && workingDir.exists()) {
                pb.directory(workingDir)
            }
            pb.redirectErrorStream(true)

            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            val exitCode = process.waitFor()
            CommandResult(output.toString().trimEnd(), exitCode)
        } catch (e: Exception) {
            CommandResult("Error: ${e.message}", -1)
        }
    }

    // ── Execute inside PRoot if bootstrapped ─────────────────────
    suspend fun executeInProot(
        command: String,
        workingDir: File? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        if (!isBootstrapped()) {
            return@withContext CommandResult("PRoot not bootstrapped. Run bootstrap first.", -1)
        }
        try {
            val prootCmd = "${prootBinary.absolutePath} --link2symlink -0 " +
                "-r ${rootfsDir.absolutePath} " +
                "-b /dev -b /sys -b /proc " +
                "-w /root " +
                "/bin/sh -c \"$command\""

            val pb = ProcessBuilder("sh", "-c", prootCmd)
            if (workingDir != null && workingDir.exists()) {
                pb.directory(workingDir)
            }
            pb.redirectErrorStream(true)

            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            val exitCode = process.waitFor()
            CommandResult(output.toString().trimEnd(), exitCode)
        } catch (e: Exception) {
            CommandResult("Error: ${e.message}", -1)
        }
    }

    // ── Bootstrap PRoot Ubuntu/Alpine ────────────────────────────
    suspend fun bootstrap(
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!rootfsDir.exists()) rootfsDir.mkdirs()

            onProgress("Detecting CPU architecture...")
            val arch = System.getProperty("os.arch") ?: "aarch64"
            val alpineArch = if (arch.contains("aarch64") || arch.contains("arm64")) "aarch64" else "x86_64"

            // Download Alpine rootfs
            onProgress("Downloading Alpine Linux rootfs ($alpineArch)...")
            val rootfsUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/$alpineArch/alpine-minirootfs-3.19.1-$alpineArch.tar.gz"
            val tarFile = File(context.cacheDir, "rootfs.tar.gz")

            downloadFile(rootfsUrl, tarFile)
            onProgress("Extracting rootfs...")

            val extractProcess = ProcessBuilder("tar", "-xf", tarFile.absolutePath, "-C", rootfsDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            extractProcess.waitFor()
            tarFile.delete()

            // Download proot static binary
            onProgress("Downloading PRoot binary...")
            val prootUrl = "https://proot.gitlab.io/proot/bin/proot-$alpineArch"
            downloadFile(prootUrl, prootBinary)
            prootBinary.setExecutable(true)

            // Write DNS config so networking works inside proot
            val resolvConf = File(rootfsDir, "etc/resolv.conf")
            resolvConf.parentFile?.mkdirs()
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")

            onProgress("Bootstrap complete!")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadFile(urlStr: String, dest: File) {
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connect()
        conn.inputStream.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
