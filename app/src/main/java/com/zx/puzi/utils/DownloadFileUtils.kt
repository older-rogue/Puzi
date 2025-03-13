package com.zx.puzi.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.*

object DownloadFileUtils {

    /**
     * 将文本内容保存到 Download 目录
     * @param context 上下文
     * @param fileName 文件名（包含扩展名，如 example.txt）
     * @param content 要写入的内容
     * @return Uri? 返回文件的 Uri，失败返回 null
     */
    fun saveFileToDownload(
        context: Context,
        folderName: String,
        fileName: String,
        content: String
    ): Uri? {
        val resolver = context.contentResolver

        // **📌 1. 先检查文件是否已存在**
        val existingUri = getExistingFileUri(context, fileName)
        existingUri?.let {
            // **删除已存在的文件**
            resolver.delete(it, null, null)
        }

        // **📌 2. 创建新文件**
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)  // 文件名
            put(MediaStore.Downloads.MIME_TYPE, "text/plain") // 文件类型
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$folderName/"
            ) // 下载目录
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()
                }
                return uri // **📌 返回新文件的 URI**
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * **📌 检查 `Download` 目录中是否已存在同名文件**
     * @return 如果文件存在，返回 URI，否则返回 `null`
     */
    private fun getExistingFileUri(context: Context, fileName: String): Uri? {
        val resolver = context.contentResolver
        val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build()
            }
        }
        return null
    }


    /**
     * 读取 Download 目录下的文件内容
     * @param context 上下文
     * @param fileName 要读取的文件名
     * @return 文件内容（字符串），如果失败则返回 null
     */
    fun readFileFromDownload(fileName: String): String? {
        val file = File(Environment.getExternalStorageDirectory(), "Download/$fileName.txt")
        if (file.exists()) {
            return file.readText()
        }
        return null
    }


    /**
     * 列出 Download 目录中的所有文件
     * @param context 上下文
     * @return List<String> 文件名列表
     */
    fun listDownloadFiles(context: Context): List<String> {
        val fileList = mutableListOf<String>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                fileList.add(name)
            }
        }
        return fileList
    }
}
