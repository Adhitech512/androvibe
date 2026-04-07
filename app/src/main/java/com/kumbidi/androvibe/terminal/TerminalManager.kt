package com.kumbidi.androvibe.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class TerminalManager(private val context: Context) {

    private val rootfsDir: File by lazy {
        context.getDir("rootfs", Context.MODE_PRIVATE)
    }

    private val prootBinary: File by lazy {
        File(context.getDir("bin", Context.MODE_PRIVATE), "proot")
    }

    /**
     * Checks if the PRoot and rootfs are already installed.
     */
    fun isTerminalReady(): Boolean {
        // Simple check to see if the basic directories of linux rootfs exist
        val binDir = File(rootfsDir, "bin")
        return binDir.exists() && prootBinary.exists()
    }

    /**
     * Downloads a basic Alpine Linux rootfs tarball and the proot binary.
     * This makes the app fully standalone as requested.
     */
    suspend fun bootstrapEnvironment(
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!rootfsDir.exists()) rootfsDir.mkdirs()
            val binDir = context.getDir("bin", Context.MODE_PRIVATE)

            // 1. Download Alpine rootfs (example architecture link, would dynamically pick based on ABI)
            val rootfsUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz"
            val tarFile = File(context.cacheDir, "rootfs.tar.gz")
            
            downloadFile(rootfsUrl, tarFile, onProgress = { progress -> 
                onProgress((progress * 0.5f).toInt()) // First 50%
            })

            // 2. Download statically compiled proot binary for Android
            // Assuming we host a reliable static proot release or use termux-packages repo
            val prootUrl = "https://github.com/proot-me/proot/releases/download/v5.3.0/proot-v5.3.0-aarch64-static"
            downloadFile(prootUrl, prootBinary)
            prootBinary.setExecutable(true)
            
            onProgress(60)

            // 3. Extract rootfs using Android's built-in toybox tar
            val process = ProcessBuilder("tar", "-xf", tarFile.absolutePath, "-C", rootfsDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                return@withContext Result.failure(Exception("Tar extraction failed with code: $exitCode"))
            }
            
            tarFile.delete() // Clean up

            // 4. Create initial bash wrapper script to launch PRoot
            val launcherScript = File(binDir, "start_terminal.sh")
            launcherScript.writeText(
                """
                #!/system/bin/sh
                ${prootBinary.absolutePath} --link2symlink -0 -r ${rootfsDir.absolutePath} -b /dev -b /sys -b /proc -w /root /bin/sh -l
                """.trimIndent()
            )
            launcherScript.setExecutable(true)

            onProgress(100)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadFile(urlStr: String, destination: File, onProgress: ((Int) -> Unit)? = null) {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        val lengthOfFile = connection.contentLength
        val input = connection.inputStream
        val output = FileOutputStream(destination)

        val data = ByteArray(4096)
        var total: Long = 0
        var count: Int
        
        while (input.read(data).also { count = it } != -1) {
            total += count.toLong()
            onProgress?.invoke(((total * 100) / lengthOfFile).toInt())
            output.write(data, 0, count)
        }

        output.flush()
        output.close()
        input.close()
    }
}
