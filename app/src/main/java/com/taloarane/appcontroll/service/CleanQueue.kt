package com.taloarane.appcontroll.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CleanAction { FORCE_STOP, CLEAR_CACHE, UNINSTALL }

data class CleanTask(val packageName: String, val label: String, val action: CleanAction)

data class CleanProgress(
    val running: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val label: String = "",
    val succeeded: List<String> = emptyList(),
    val failed: List<String> = emptyList(),
)

/** Shared state between the UI, the foreground service and the accessibility service. */
object CleanQueue {

    private val _progress = MutableStateFlow(CleanProgress())
    val progress = _progress.asStateFlow()

    @Volatile
    var pending: List<CleanTask> = emptyList()
        private set

    @Volatile
    var currentTask: CleanTask? = null
        private set

    @Volatile
    private var waiter: CompletableDeferred<Boolean>? = null

    @Volatile
    var cancelled: Boolean = false
        private set

    fun submit(tasks: List<CleanTask>) {
        pending = tasks
        cancelled = false
        _progress.value = CleanProgress(running = true, current = 0, total = tasks.size)
    }

    fun beginTask(task: CleanTask, index: Int): CompletableDeferred<Boolean> {
        currentTask = task
        val deferred = CompletableDeferred<Boolean>()
        waiter = deferred
        _progress.value = _progress.value.copy(current = index + 1, label = task.label)
        return deferred
    }

    /** Called by the accessibility service once the automation finished (or gave up). */
    fun completeCurrent(success: Boolean) {
        val task = currentTask ?: return
        currentTask = null
        val p = _progress.value
        _progress.value = if (success) {
            p.copy(succeeded = p.succeeded + task.label)
        } else {
            p.copy(failed = p.failed + task.label)
        }
        waiter?.takeIf { !it.isCompleted }?.complete(success)
        waiter = null
    }

    fun cancel() {
        cancelled = true
        pending = emptyList()
        completeCurrent(false)
    }

    fun finish() {
        currentTask = null
        pending = emptyList()
        _progress.value = _progress.value.copy(running = false, label = "")
    }

    fun clearSummary() {
        _progress.value = CleanProgress()
    }
}
