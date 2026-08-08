package com.taloarane.appcontroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.core.AppItem
import com.taloarane.appcontroll.core.AppRepo
import com.taloarane.appcontroll.core.FileCategory
import com.taloarane.appcontroll.core.LocalL
import com.taloarane.appcontroll.core.RamStats
import com.taloarane.appcontroll.core.ScannedFile
import com.taloarane.appcontroll.core.StorageScan
import com.taloarane.appcontroll.core.VolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun InfoRamScreen(stats: RamStats, apps: List<AppItem>, onStop: (AppItem) -> Unit) {
    val l = LocalL.current
    val extra = LocalExtra.current
    val active = apps.filter { it.active }.sortedByDescending { it.lastUsed }
    val share = if (active.isEmpty()) 0L else stats.userApps / active.size

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AcCard(Modifier.fillMaxWidth(), accent = NeonPurple) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        l.infoRam,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${AppRepo.formatBytes(stats.used)} / ${AppRepo.formatBytes(stats.total)} · " +
                            "${active.size} ${l.running.lowercase()}",
                        fontSize = 12.sp,
                        color = extra.subtle
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressBarLine(
                        stats.used.toFloat() / stats.total.coerceAtLeast(1),
                        NeonPurple,
                        height = 8.dp
                    )
                }
            }
        }
        items(active, key = { it.packageName }) { app ->
            AcCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${AppRepo.formatBytes(share)} (${l.estimated})",
                            fontSize = 11.sp,
                            color = extra.subtle
                        )
                    }
                    ActionChip(l.stop, NeonRed) { onStop(app) }
                }
            }
        }
    }
}

@Composable
fun StorageScreen(title: String, volume: VolumeInfo?, missingText: String) {
    val l = LocalL.current
    val extra = LocalExtra.current
    var loading by remember(volume?.root) { mutableStateOf(true) }
    var files by remember(volume?.root) { mutableStateOf<List<ScannedFile>>(emptyList()) }
    var openCategory by remember { mutableStateOf<FileCategory?>(null) }
    var confirmDelete by remember { mutableStateOf<File?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(volume?.root, reload) {
        loading = true
        files = withContext(Dispatchers.IO) { StorageScan.scan(volume?.root) }
        loading = false
    }

    if (volume?.root == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { Text(missingText, color = extra.subtle) }
        return
    }

    val grouped = files.groupBy { it.category }
    val cat = openCategory

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AcCard(Modifier.fillMaxWidth(), accent = NeonCyan) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${AppRepo.formatBytes(volume.used)} / ${AppRepo.formatBytes(volume.total)}",
                        fontSize = 12.sp,
                        color = extra.subtle
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressBarLine(
                        volume.used.toFloat() / volume.total.coerceAtLeast(1),
                        NeonCyan,
                        height = 8.dp
                    )
                }
            }
        }

        if (loading) {
            item {
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = NeonPurple)
                }
            }
        }

        if (cat == null) {
            items(FileCategory.entries.toList()) { c ->
                val list = grouped[c].orEmpty()
                if (list.isNotEmpty()) {
                    AcCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { openCategory = c }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    categoryLabel(c),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("${list.size} file", fontSize = 11.sp, color = extra.subtle)
                            }
                            Text(
                                AppRepo.formatBytes(list.sumOf { it.size }),
                                fontWeight = FontWeight.Black,
                                color = categoryColor(c)
                            )
                        }
                    }
                }
            }
        } else {
            item {
                ActionChip("← ${categoryLabel(cat)}", NeonPurple, Modifier.fillMaxWidth()) {
                    openCategory = null
                }
            }
            items(grouped[cat].orEmpty(), key = { it.file.absolutePath }) { sf ->
                AcCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                sf.file.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                sf.file.parent ?: "",
                                fontSize = 10.sp,
                                color = extra.subtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                AppRepo.formatBytes(sf.size),
                                fontSize = 11.sp,
                                color = categoryColor(sf.category),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ActionChip(l.delete, NeonRed) { confirmDelete = sf.file }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }

    confirmDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(l.delete, fontWeight = FontWeight.Bold) },
            text = { Text("${file.name}\n\n${l.deleteConfirm}", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    StorageScan.delete(file)
                    confirmDelete = null
                    reload++
                }) { Text(l.delete, color = NeonRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(l.cancel) } },
            shape = CardShape,
            containerColor = extra.cardBg
        )
    }
}

@Composable
private fun categoryLabel(c: FileCategory): String {
    val l = LocalL.current
    return when (c) {
        FileCategory.VIDEO -> l.video
        FileCategory.IMAGE -> l.images
        FileCategory.AUDIO -> l.audio
        FileCategory.DOCUMENT -> l.documents
        FileCategory.APK -> l.apks
        FileCategory.GAME -> l.games
        FileCategory.APP -> l.apps
        FileCategory.SYSTEM -> l.systemFiles
        FileCategory.OTHER -> "Other"
    }
}

private fun categoryColor(c: FileCategory) = when (c) {
    FileCategory.VIDEO -> NeonPurple
    FileCategory.IMAGE -> NeonCyan
    FileCategory.AUDIO -> NeonGreen
    FileCategory.DOCUMENT -> NeonBlue
    FileCategory.APK -> NeonYellow
    FileCategory.GAME -> NeonRed
    FileCategory.APP -> NeonCyan
    FileCategory.SYSTEM -> NeonYellow
    FileCategory.OTHER -> NeonBlue
}
