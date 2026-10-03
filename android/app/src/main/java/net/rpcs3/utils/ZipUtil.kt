package net.rpcs3.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ZipUtil {
    private const val MAX_ENTRIES = 10_000
    private const val MAX_ENTRY_BYTES = 512L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 1024L * 1024L * 1024L
    private const val COPY_BUFFER_BYTES = 128 * 1024

    @Throws(IOException::class)
    fun unzip(file: File, targetDirectory: File) {
        var entries = 0
        var totalBytes = 0L

        ZipFile(file).use { zipFile ->
            for (zipEntry in zipFile.entries()) {
                if (++entries > MAX_ENTRIES) {
                    throw IOException("Archive contains too many entries")
                }

                validateDeclaredSize(zipEntry)

                val destFile = createNewFile(targetDirectory, zipEntry)
                val destDirectory = if (zipEntry.isDirectory) destFile else destFile.parentFile

                if (destDirectory == null || (!destDirectory.isDirectory && !destDirectory.mkdirs())) {
                    throw FileNotFoundException("Failed to create destination directory: $destDirectory")
                }

                if (zipEntry.isDirectory) continue

                try {
                    zipFile.getInputStream(zipEntry).use { inputStream ->
                        BufferedOutputStream(destFile.outputStream(), COPY_BUFFER_BYTES).use { output ->
                            totalBytes = copyEntry(inputStream, output, totalBytes)
                        }
                    }
                } catch (e: IOException) {
                    destFile.delete()
                    throw e
                }
            }
        }
    }

    @Throws(IOException::class)
    fun unzip(stream: InputStream, targetDirectory: File) {
        var entries = 0
        var totalBytes = 0L

        ZipInputStream(BufferedInputStream(stream, COPY_BUFFER_BYTES)).use { zis ->
            while (true) {
                val zipEntry = zis.nextEntry ?: break
                if (++entries > MAX_ENTRIES) {
                    throw IOException("Archive contains too many entries")
                }

                validateDeclaredSize(zipEntry)

                val destFile = createNewFile(targetDirectory, zipEntry)
                val destDirectory = if (zipEntry.isDirectory) destFile else destFile.parentFile

                if (destDirectory == null || (!destDirectory.isDirectory && !destDirectory.mkdirs())) {
                    throw FileNotFoundException("Failed to create destination directory: $destDirectory")
                }

                if (!zipEntry.isDirectory) {
                    try {
                        BufferedOutputStream(destFile.outputStream(), COPY_BUFFER_BYTES).use { output ->
                            totalBytes = copyEntry(zis, output, totalBytes)
                        }
                    } catch (e: IOException) {
                        destFile.delete()
                        throw e
                    }
                }

                zis.closeEntry()
            }
        }
    }

    @Throws(IOException::class)
    private fun copyEntry(input: InputStream, output: OutputStream, currentTotal: Long): Long {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var entryBytes = 0L
        var totalBytes = currentTotal

        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break

            entryBytes += read
            totalBytes += read

            if (entryBytes > MAX_ENTRY_BYTES) {
                throw IOException("Archive entry exceeds the extraction size limit")
            }
            if (totalBytes > MAX_TOTAL_BYTES) {
                throw IOException("Archive exceeds the total extraction size limit")
            }

            output.write(buffer, 0, read)
        }

        output.flush()
        return totalBytes
    }

    @Throws(IOException::class)
    private fun validateDeclaredSize(entry: ZipEntry) {
        if (entry.size > MAX_ENTRY_BYTES) {
            throw IOException("Archive entry is too large: ${entry.name}")
        }
    }

    @Throws(IOException::class)
    private fun createNewFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)
        val destDirPath = destinationDir.canonicalFile
        val destFilePath = destFile.canonicalFile

        if (destFilePath != destDirPath &&
            !destFilePath.path.startsWith(destDirPath.path + File.separator)
        ) {
            throw IOException("Entry is outside of the target dir: ${zipEntry.name}")
        }

        return destFilePath
    }
}
