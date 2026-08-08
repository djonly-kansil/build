package com.taloarane.appcontroll.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taloarane.appcontroll.data.AppEntry
import com.taloarane.appcontroll.data.AppRepository
import com.taloarane.appcontroll.data.CategorySummary
import com.taloarane.appcontroll.data.FileItem
import com.taloarane.appcontroll.data.Prefs
import com.taloarane.appcontroll.data.RamRepository
import com.taloarane.appcontroll.data.RamStats
import com.taloarane.appcontroll.data.Settings
import com.taloarane.appcontroll.data.StorageCategory
import com.taloarane.appcontroll.data.StorageRepository
import com.taloarane.appcontroll.data.ThemeMode
import com.taloarane.appcontroll.data.VolumeInfo
import com.taloarane.appcontroll.service.CleanAction
import com.taloarane.appcontroll.service.CleanQueue
import com.taloarane.appcontroll.service.CleanTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppFilter { ACTIVE, ALL, INACTIVE }
enum class AppTab { USER, SYSTEM, WHITELIST }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    val settings: StateFlow<Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _ram = MutableStateFlow(RamStats())
    val ram = _ram.asStateFlow()

    private val _history = MutableStateFlow(List(60) { 0f })
    val history = _history.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _loadingApps = MutableStateFlow(false)
    val loadingApps = _loadingApps.asStateFlow()

    private val _filter = MutableStateFlow(AppFilter.ACTIVE)
    val filter = _filter.asStateFlow()

    private val _tab = MutableStateFlow(AppTab.USER)
    val tab = _tab.asStateFlow()

    private val _romVolume = MutableStateFlow(StorageRepository.internalVolume())
    val romVolume = _romVolume.asStateFlow()

    private val _sdVolume = MutableStateFlow<VolumeInfo?>(null)
    val sdVolume = _sdVolume.asStateFlow()

    private val _scan = MutableStateFlow<Map<StorageCategory, List<FileItem>>>(emptyMap())
    val scan = _scan.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    val cleanProgress = CleanQueue.progress

    init {
        viewModelScope.launch {
            while (true) {
                val stats = withContext(Dispatchers.IO) { RamRepository.read(getApplication()) }
                _ram.value = stats
                _history.value = (_history.value.drop(1) + stats.usedPercent.toFloat())
                delay(1000)
            }
        }
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            _loadingApps.value = true
            _apps.value = withContext(Dispatchers.IO) { AppRepository.loadApps(getApplication()) }
            _loadingApps.value = false
        }
    }

    fun setFilter(value: AppFilter) { _filter.value = value }
    fun setTab(value: AppTab) { _tab.value = value }

    fun setLanguage(value: String) = viewModelScope.launch { prefs.setLanguage(value) }
    fun setTheme(value: ThemeMode) = viewModelScope.launch { prefs.setTheme(value) }
    fun setKeepScreenOn(value: Boolean) = viewModelScope.launch { prefs.setKeepScreenOn(value) }
    fun setOnboarded(value: Boolean) = viewModelScope.launch { prefs.setOnboarded(value) }
    fun toggleWhitelist(pkg: String) = viewModelScope.launch { prefs.toggleWhitelist(pkg) }

    fun visibleApps(): List<AppEntry> {
        val whitelist = settings.value.whitelist
        val byTab = when (_tab.value) {
            AppTab.USER -> _apps.value.filter { !it.isSystem && it.packageName !in whitelist }
            AppTab.SYSTEM -> _apps.value.filter { it.isSystem && it.packageName !in whitelist }
            AppTab.WHITELIST -> _apps.value.filter { it.packageName in whitelist }
        }
        return when (_filter.value) {
            AppFilter.ACTIVE -> byTab.filter { it.running }
            AppFilter.INACTIVE -> byTab.filter { !it.running }
            AppFilter.ALL -> byTab
        }
    }

    fun queue(entries: List<AppEntry>, action: CleanAction) {
        val whitelist = settings.value.whitelist
        val tasks = entries
            .filter { it.packageName !in whitelist || _tab.value == AppTab.WHITELIST }
            .map { CleanTask(it.packageName, it.label, action) }
        if (tasks.isEmpty()) return
        CleanQueue.submit(tasks)
    }

    fun bulkTargets(kind: AppTab, action: CleanAction): List<AppEntry> {
        val whitelist = settings.value.whitelist
        return _apps.value.filter { entry ->
            entry.packageName !in whitelist &&
                entry.risk != com.taloarane.appcontroll.data.Risk.DANGER &&
                when (kind) {
                    AppTab.USER -> !entry.isSystem
                    AppTab.SYSTEM -> entry.isSystem
                    AppTab.WHITELIST -> false
                } &&
                (action != CleanAction.CLEAR_CACHE || entry.cacheBytes > 0 || true) &&
                entry.running
        }
    }

    fun scanStorage(volume: VolumeInfo?) {
        viewModelScope.launch {
            _scanning.value = true
            _scan.value = withContext(Dispatchers.IO) { StorageRepository.scan(volume?.root) }
            _scanning.value = false
        }
    }

    fun refreshVolumes() {
        _romVolume.value = StorageRepository.internalVolume()
        _sdVolume.value = StorageRepository.sdCardVolume(getApplication())
    }

    fun deleteFiles(paths: Collection<String>, volume: VolumeInfo?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { StorageRepository.delete(paths) }
            scanStorage(volume)
        }
    }

    fun summaries(): List<CategorySummary> = StorageRepository.summarize(_scan.value)
}
