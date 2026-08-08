package com.taloarane.appcontroll.data

import android.content.Context
import android.os.Environment
import java.io.File

enum class StorageCategory(val labelId: String, val labelEn: String, val extensions: List<String>) {
    VIDEO("Video", "Video", listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv")),
    APPS("Aplikasi", "Apps", listOf("apk")),
    GAME("Game", "Games", listOf("obb", "sav")),
    DOCUMENT("Dokumen", "Documents", listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub")),
    IMAGE("Gambar", "Images", listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")),
    AUDIO("Audio", "Audio", listOf("mp3", "aac", "wav", "ogg", "m4a", "flac", "opus")),
    APK("APK/APKS", "APK/APKS", listOf("apk", "apks", "xapk", "apkm")),
    SYSTEM("Sistem", "System", listOf("log", "tmp", "cache", "dat", "bin")),
}

data class FileItem(val path: String, val name: String, val sizeBytes: Long)

data class CategorySummary(val category: StorageCategory, val count: Int, val sizeBytes: Long)

data class VolumeInfo(val label: String, val root: String?, val totalBytes: Long, val freeBytes: Long)

object StorageRepository {

    fun internalVolume(): VolumeInfo {
        val root = Environment.getExternalStorageDirectory()
        return VolumeInfo("ROM", root?.absolutePath, root?.totalSpace ?: 0L, root?.freeSpace ?: 0L)
    }

    fun sdCardVolume(context: Context): VolumeInfo? {
        val internal = Environment.getExternalStorageDirectory()?.absolutePath
        val dirs = context.getExternalFilesDirs(null).filterNotNull()
        val external = dirs.map { dir ->
            var f: File = dir
            repeat(4) { f = f.parentFile ?: f }
            f
        }.firstOrNull { it.absolutePath != internal && it.exists() } ?: return null
        return VolumeInfo("SD Card", external.absolutePath, external.totalSpace, external.freeSpace)
    }

    fun scan(root: String?): Map<StorageCategory, List<FileItem>> {
        if (root == null) return emptyMap()
        val result = StorageCategory.entries.associateWith { mutableListOf<FileItem>() }
        val stack = ArrayDeque<File>()
        stack.add(File(root))
        var visited = 0
        while (stack.isNotEmpty() && visited < 60_000) {
            val current = stack.removeFirst()
            val children = current.listFiles() ?: continue
            for (child in children) {
                visited++
                if (child.isDirectory) {
                    if (!child.name.startsWith(".")) stack.add(child)
                    continue
                }
                val ext = child.extension.lowercase()
                StorageCategory.entries.forEach { cat ->
                    if (ext in cat.extensions) {
                        result[cat]?.add(FileItem(child.absolutePath, child.name, child.length()))
                    }
                }
            }
        }
        return result.mapValues { (_, list) -> list.sortedByDescending { it.sizeBytes } }
    }

    fun summarize(scan: Map<StorageCategory, List<FileItem>>): List<CategorySummary> =
        StorageCategory.entries.map { cat ->
            val list = scan[cat].orEmpty()
            CategorySummary(cat, list.size, list.sumOf { it.sizeBytes })
        }

    fun delete(paths: Collection<String>): Int = paths.count { runCatching { File(it).delete() }.getOrDefault(false) }
}
