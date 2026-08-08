package com.taloarane.appcontroll.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taloarane.appcontroll.data.AppRepository
import com.taloarane.appcontroll.data.FileItem
import com.taloarane.appcontroll.data.StorageCategory
import com.taloarane.appcontroll.data.VolumeInfo
import com.taloarane.appcontroll.ui.LocalStrings
import com.taloarane.appcontroll.ui.components.SectionCard
import com.taloarane.appcontroll.ui.components.ThinBar
import com.taloarane.appcontroll.ui.theme.NeonCyan
import com.taloarane.appcontroll.ui.theme.NeonRed

@Composable
fun StorageScreen(
    volume: VolumeInfo?,
    language: String,
    scan: Map<StorageCategory, List<FileItem>>,
    scanning: Boolean,
    onScan: () -> Unit,
    onDelete: (Collection<String>) -> Unit,
) {
    val s = LocalStrings.current
    var openCategory by remember { mutableStateOf<StorageCategory?>(null) }
    val selected = remember { mutableStateListOf<String>() }

    LaunchedEffect(volume?.root) { onScan() }

    if (volume == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            Text(s.emptyList, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val category = openCategory
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard(accent = NeonCyan) {
                Text(volume.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${AppRepository.formatBytes(volume.totalBytes - volume.freeBytes)} / ${AppRepository.formatBytes(volume.totalBytes)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                val used = if (volume.totalBytes <= 0) 0 else
                    (((volume.totalBytes - volume.freeBytes) * 100) / volume.totalBytes).toInt()
                ThinBar(percent = used, color = NeonCyan)
                Spacer(Modifier.height(4.dp))
                Text("${s.storageFree}: ${AppRepository.formatBytes(volume.freeBytes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (scanning) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        if (category == null) {
            items(StorageCategory.entries.toList(), key = { it.name }) { cat ->
                val list = scan[cat].orEmpty()
                SectionCard(modifier = Modifier.clickable { openCategory = cat }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (language == "en") cat.labelEn else cat.labelId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text("${list.size} file", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            AppRepository.formatBytes(list.sumOf { it.sizeBytes }),
                            fontSize = 13.sp,
                            color = NeonCyan,
                        )
                    }
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "← " + (if (language == "en") category.labelEn else category.labelId),
                        modifier = Modifier.weight(1f).clickable {
                            openCategory = null
                            selected.clear()
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (selected.isNotEmpty()) {
                        ActionPill("${s.deleteAll} (${selected.size})", NeonRed) {
                            onDelete(selected.toList())
                            selected.clear()
                        }
                    }
                }
            }
            items(scan[category].orEmpty(), key = { it.path }) { file ->
                SectionCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = file.path in selected,
                            onCheckedChange = {
                                if (file.path in selected) selected.remove(file.path) else selected.add(file.path)
                            },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                file.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(AppRepository.formatBytes(file.sizeBytes), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ActionPill(s.delete, NeonRed) { onDelete(listOf(file.path)) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
