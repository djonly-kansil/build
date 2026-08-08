package com.taloarane.appcontroll.service

import kotlinx.coroutines.flow.MutableStateFlow

enum class CleanAction { FORCE_STOP, CLEAR_CACHE, UNINSTALL }

data class CleanTask(
    val packageName: String,
    val label: String,
    val actions: List<CleanAction>,
)

data class CleanState(
    val running: Boolean = false,
    val index: Int = 0,
    val total: Int = 0,
    val current: String = "",
    val succeeded: Int = 0,
    val failed: Int = 0,
    val finishedMessage: String? = null,
)

object CleanEngine {
    val state = MutableStateFlow(CleanState())
    @Volatile
    var cancelRequested: Boolean = false
}
