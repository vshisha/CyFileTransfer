package com.cyfiletransfer.app

import android.content.Context
import android.net.Uri
import java.io.File

data class StoredFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)

object FileStorage {

    private const val FOLDER_NAME = "PhoneTransfer"

    fun getFolder(context: Context): File? {
        return try {
            val parentFolder = context.getExternalFilesDir(null)
                ?: return null

            val folder = File(parentFolder, FOLDER_NAME)

            if (!folder.exists()) {
                folder.mkdirs()
            }

            if (folder.exists() && folder.isDirectory) {
                folder
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun ensureFolder(context: Context): Boolean {
        return getFolder(context) != null
    }

    fun getFiles(context: Context): List<StoredFile> {
        val folder = getFolder(context) ?: return emptyList()

        return try {
            folder.listFiles()
                ?.filter { it.isFile }
                ?.map { file ->
                    StoredFile(
                        uri = Uri.fromFile(file),
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        mimeType = null
                    )
                }
                ?.sortedByDescending { it.lastModified }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteFile(context: Context, fileName: String): Boolean {
        val folder = getFolder(context) ?: return false

        return try {
            val file = File(folder, fileName)

            if (
                file.isFile &&
                file.canonicalFile.parentFile == folder.canonicalFile
            ) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
