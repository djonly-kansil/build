package com.taloarane.appcontroll.core

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

enum class FileCategory { VIDEO, IMAGE, AUDIO, DOCUMENT, APK, GAME, APP, SYSTEM, OTHER }

data class ScannedFile(val file: File, val size: Long, val category: FileCategory)

data class VolumeInfo(val root: File?, val total: Long, val free: Long, val used: Long)

object StorageScan {

    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "ts")
    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    private val audioExt = setOf("mp3", "aac", "wav", "ogg", "m4a", "flac", "opus", "amr")
    private val docExt = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub", "csv", "zip", "rar", "7z")
    private val apkExt = setOf("apk", "apks", "xapk", "apkm", "obb")

    fun internal(): VolumeInfo = volume(Environment.getExternalStorageDirectory())

    fun sdCard(context: Context): VolumeInfo? {
        val dirs = context.getExternalFilesDirs(null).filterNotNull()
        val removable = dirs.drop(1).firstOrNull() ?: return null
        // /storage/XXXX-XXXX/Android/data/<pkg>/files -> /storage/XXXX-XXXX
        var root: File? = removable
        repeat(4) { root = root?.parentFile }
        return volume(root)
    }

    fun volume(root: File?): VolumeInfo {
        if (root == null || !root.exists()) return VolumeInfo(null, 0, 0, 0)
        return runCatching {
            val st = StatFs(root.absolutePath)
            val total = st.blockCountLong * st.blockSizeLong
            val free = st.availableBlocksLong * st.blockSizeLong
            VolumeInfo(root, total, free, total - free)
        }.getOrDefault(VolumeInfo(root, 0, 0, 0))
    }

    fun categoryOf(file: File): FileCategory {
        val ext = file.extension.lowercase()
        val path = file.absolutePath.lowercase()
        return when {
            ext in apkExt -> FileCategory.APK
            ext in videoExt -> FileCategory.VIDEO
            ext in imageExt -> FileCategory.IMAGE
            ext in audioExt -> FileCategory.AUDIO
            ext in docExt -> FileCategory.DOCUMENT
            path.contains("/android/obb") || path.contains("/games") -> FileCategory.GAME
            path.contains("/android/data") -> FileCategory.APP
            path.contains("/android/") -> FileCategory.SYSTEM
            else -> FileCategory.OTHER
        }
    }

    /** Walks the volume and returns the biggest files, grouped later by category. */
    fun scan(root: File?, maxFiles: Int = 4000, minSize: Long = 1L * 1024 * 1024): List<ScannedFile> {
        if (root == null || !root.exists()) return emptyList()
        val out = ArrayList<ScannedFile>(1024)
        val stack = ArrayDeque<File>()
        stack.add(root)
        var visited = 0
        while (stack.isNotEmpty() && out.size < maxFiles && visited < 250_000) {
            val dir = stack.removeLast()
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (f in children) {
                visited++
                if (f.isDirectory) {
                    if (!f.name.startsWith(".")) stack.add(f)
                } else {
                    val size = f.length()
                    if (size >= minSize) out.add(ScannedFile(f, size, categoryOf(f)))
                }
            }
        }
        return out.sortedByDescending { it.size }
    }

    fun delete(file: File): Boolean = runCatching {
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }.getOrDefault(false)
}
