package com.hoang.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class HoangViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var saveJob: Job? = null
    private var calculationJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { repository.load() }
            val seasons = saved.seasons
            val open = saved.openSeasonIds.filter { id -> seasons.any { it.id == id } }
            val selected = saved.selectedSeasonId?.takeIf { id -> seasons.any { it.id == id } } ?: open.firstOrNull() ?: seasons.firstOrNull()?.id
            _state.value = _state.value.copy(
                loaded = true,
                splashVisible = saved.settings.splashEnabled,
                settings = saved.settings,
                seasons = seasons,
                openSeasonIds = open,
                selectedSeasonId = selected
            )
            if (saved.settings.seasonSort.isNotBlank()) sortSeasons(saved.settings.seasonSort)
            scheduleCalculation()
            if (saved.settings.forceKeepAlive) KeepAliveService.start(application)
        }
    }

    fun finishSplash() { _state.value = _state.value.copy(splashVisible = false) }

    private fun updateState(transform: (UiState) -> UiState) {
        _state.value = transform(_state.value)
        scheduleCalculation()
        scheduleSave()
    }

    private fun scheduleCalculation() {
        calculationJob?.cancel()
        val season = currentSeason(_state.value)
        calculationJob = viewModelScope.launch {
            val result = if (season == null) null else withContext(Dispatchers.Default) { Calculator.calculate(season) }
            val state = _state.value
            val updatedSeasons = if (season == null || result == null) state.seasons else state.seasons.map {
                if (it.id == season.id) it.copy(totalExpenses = result.totalExpenses, totalIncome = result.totalIncome, profit = result.profit, roiPercent = result.roi) else it
            }
            _state.value = state.copy(calculation = result, seasons = updatedSeasons)
        }
    }

    private fun currentSeason(state: UiState): Season? =
        state.seasons.firstOrNull { it.id == state.selectedSeasonId }

    private fun updateSeason(transform: (Season) -> Season) {
        updateState { state ->
            val current = currentSeason(state) ?: return@updateState state
            val before = current.copy(editedAt = current.editedAt)
            val updated = transform(current).copy(editedAt = System.currentTimeMillis(), temporary = false)
            state.copy(
                seasons = state.seasons.map { if (it.id == current.id) updated else it },
                undo = (listOf(before) + state.undo).take(30),
                redo = emptyList()
            )
        }
    }

    fun undo() = updateState { state ->
        val current = currentSeason(state) ?: return@updateState state
        val previous = state.undo.firstOrNull() ?: return@updateState state
        state.copy(
            seasons = state.seasons.map { if (it.id == current.id) previous else it },
            undo = state.undo.drop(1),
            redo = (listOf(current) + state.redo).take(30)
        )
    }

    fun redo() = updateState { state ->
        val current = currentSeason(state) ?: return@updateState state
        val next = state.redo.firstOrNull() ?: return@updateState state
        state.copy(
            seasons = state.seasons.map { if (it.id == current.id) next else it },
            redo = state.redo.drop(1),
            undo = (listOf(current) + state.undo).take(30)
        )
    }

    fun setScreen(screen: String) { updateState { it.copy(screen = screen) } }
    fun setSearch(search: String) { _state.value = _state.value.copy(search = search) }
    fun selectSeason(id: String) { updateState { it.copy(selectedSeasonId = id, openSeasonIds = if (id in it.openSeasonIds) it.openSeasonIds else it.openSeasonIds + id, screen = "home") } }
    fun closeSeason(id: String) { updateState { state ->
        val open = state.openSeasonIds.filter { it != id }
        val selected = if (state.selectedSeasonId == id) open.firstOrNull() else state.selectedSeasonId
        state.copy(openSeasonIds = open, selectedSeasonId = selected, screen = if (selected == null) "seasons" else state.screen)
    } }
    fun minimizeSeason(id: String) { closeSeason(id) }

    fun createSeason() {
        val settings = _state.value.settings
        val defaults = if (settings.useDefaultSeasonSettings) settings.defaultSeason else Season()
        val season = Season(
            panelName = defaults.panelName.ifBlank { "Temporary Panel" },
            description = defaults.description,
            imageUri = defaults.imageUri,
            costFeePer100 = defaults.costFeePer100,
            finalOutputValue = defaults.finalOutputValue,
            returnRate = runCatching {
                java.math.BigDecimal(defaults.returnRate.ifBlank { "0" }).coerceAtLeast(java.math.BigDecimal.ZERO).coerceAtMost(java.math.BigDecimal("99.99999999")).stripTrailingZeros().toPlainString()
            }.getOrDefault("0"),
            totalWillMade = defaults.totalWillMade,
            outputPerCraft = defaults.outputPerCraft,
            premiumActive = defaults.premiumActive,
            sellLeftover = defaults.sellLeftover,
            inputs = defaults.inputs.map { it.copy(id = java.util.UUID.randomUUID().toString()) },
            outputs = defaults.outputs.map { it.copy(id = java.util.UUID.randomUUID().toString()) },
            temporary = true
        )
        updateState { state ->
            state.copy(
                seasons = state.seasons + season,
                openSeasonIds = state.openSeasonIds + season.id,
                selectedSeasonId = season.id,
                screen = "home"
            )
        }
    }

    fun duplicateSeason(id: String) { updateState { state ->
        val source = state.seasons.firstOrNull { it.id == id } ?: return@updateState state
        val copy = source.copy(id = java.util.UUID.randomUUID().toString(), name = "${source.name} Copy", panelName = "${source.panelName} Copy", temporary = false, editedAt = System.currentTimeMillis(), inputs = source.inputs.map { it.copy(id = java.util.UUID.randomUUID().toString()) }, outputs = source.outputs.map { it.copy(id = java.util.UUID.randomUUID().toString()) })
        state.copy(seasons = state.seasons + copy)
    } }
    fun renameSeason(id: String, name: String) { updateState { state -> state.copy(seasons = state.seasons.map { if (it.id == id) it.copy(name = name, editedAt = System.currentTimeMillis()) else it }) } }
    fun updateSeasonImage(id: String, path: String) { updateState { state -> state.copy(seasons = state.seasons.map { if (it.id == id) it.copy(imageUri = path, editedAt = System.currentTimeMillis()) else it }) } }
    fun deleteSeason(id: String) { updateState { state ->
        val seasons = state.seasons.filter { it.id != id }
        val open = state.openSeasonIds.filter { it != id }
        state.copy(seasons = seasons, openSeasonIds = open, selectedSeasonId = if (state.selectedSeasonId == id) open.firstOrNull() else state.selectedSeasonId)
    } }
    fun toggleSeasonPin(id: String) { updateState { state -> state.copy(seasons = state.seasons.map { if (it.id == id) it.copy(pinned = !it.pinned, editedAt = System.currentTimeMillis()) else it }) } }
    fun sortSeasons(mode: String) { updateState { state ->
        val base = state.seasons.filter { it.temporary.not() || it.name.isNotBlank() }
        val sorted = when (mode) {
            "Name A-Z" -> base.sortedBy { (it.name.ifBlank { it.panelName }).lowercase() }
            "Name Z-A" -> base.sortedByDescending { (it.name.ifBlank { it.panelName }).lowercase() }
            "Last edited newest" -> base.sortedByDescending { it.editedAt }
            "Last edited oldest" -> base.sortedBy { it.editedAt }
            "Created newest" -> base.sortedByDescending { it.createdAt }
            "Created oldest" -> base.sortedBy { it.createdAt }
            "Profit high-low" -> base.sortedWith(compareByDescending<Season> { it.profit != null }.thenByDescending { it.profit ?: Double.NEGATIVE_INFINITY })
            "Profit low-high" -> base.sortedWith(compareByDescending<Season> { it.profit != null }.thenBy { it.profit ?: Double.POSITIVE_INFINITY })
            "Profit % high-low" -> base.sortedWith(compareByDescending<Season> { it.roiPercent != null }.thenByDescending { it.roiPercent ?: Double.NEGATIVE_INFINITY })
            "Profit % low-high" -> base.sortedWith(compareByDescending<Season> { it.roiPercent != null }.thenBy { it.roiPercent ?: Double.POSITIVE_INFINITY })
            "Cost high-low" -> base.sortedWith(compareByDescending<Season> { it.totalExpenses != null }.thenByDescending { it.totalExpenses ?: Double.NEGATIVE_INFINITY })
            "Cost low-high" -> base.sortedWith(compareByDescending<Season> { it.totalExpenses != null }.thenBy { it.totalExpenses ?: Double.POSITIVE_INFINITY })
            "Earn high-low" -> base.sortedWith(compareByDescending<Season> { it.totalIncome != null }.thenByDescending { it.totalIncome ?: Double.NEGATIVE_INFINITY })
            "Earn low-high" -> base.sortedWith(compareByDescending<Season> { it.totalIncome != null }.thenBy { it.totalIncome ?: Double.POSITIVE_INFINITY })
            "Pinned first" -> base.sortedWith(compareByDescending<Season> { it.pinned }.thenByDescending { it.editedAt })
            "Manual order" -> base.sortedBy { it.manualOrder }
            else -> base.sortedByDescending { it.editedAt }
        }
        state.copy(seasons = sorted, settings = state.settings.copy(seasonSort = mode))
    } }

    fun updatePanel(field: String, value: String) {
        updateSeason { season -> when (field) {
            "panelName" -> season.copy(panelName = value)
            "name" -> season.copy(name = value)
            "description" -> season.copy(description = value)
            "imageUri" -> season.copy(imageUri = value)
            else -> season
        } }
    }
    fun updateRecipe(field: String, value: Double?) {
        updateSeason { season -> when (field) {
            "costFee" -> season.copy(costFeePer100 = value)
            "finalValue" -> season.copy(finalOutputValue = value)
            "returnRate" -> season.copy(returnRate = value?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "0")
            "total" -> season.copy(totalWillMade = value)
            "outputPerCraft" -> season.copy(outputPerCraft = value ?: 1.0)
            "craftFee" -> season.copy(craftFee = value ?: 0.0)
            else -> season
        } }
    }
    fun toggleRecipe(field: String) { updateSeason { season -> when (field) {
        "premium" -> season.copy(premiumActive = !season.premiumActive)
        else -> season.copy(sellLeftover = !season.sellLeftover)
    } } }

    fun createInput() {
        val settings = _state.value.settings
        updateSeason { season -> season.copy(inputs = season.inputs + InputMaterial(name = "Temporary ${nextInputNumber(season)}", buyLocation = normalizeLocation(settings.lastBuyLocation), buyMethod = normalizeBuyMethod(settings.lastBuyMethod))) }
    }
    private fun nextInputNumber(season: Season) = season.inputs.size + 1
    fun updateInput(id: String, transform: (InputMaterial) -> InputMaterial) { updateSeason { season -> season.copy(inputs = season.inputs.map { if (it.id == id) transform(it) else it }) } }
    fun deleteInput(id: String) { updateSeason { season -> season.copy(inputs = season.inputs.filter { it.id != id }) } }
    fun duplicateInput(id: String) { updateSeason { season ->
        val source = season.inputs.firstOrNull { it.id == id } ?: return@updateSeason season
        val copy = source.copy(id = java.util.UUID.randomUUID().toString(), name = "${source.name} Copy", selected = false)
        season.copy(inputs = season.inputs + copy)
    } }
    fun moveInput(id: String, delta: Int) { updateSeason { season ->
        val list = season.inputs.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (index >= 0 && target >= 0 && index != target) { val item = list.removeAt(index); list.add(target, item) }
        season.copy(inputs = list)
    } }
    fun sortInputs() { updateSeason { season -> season.copy(inputs = season.inputs.sortedBy { it.name.lowercase() }) } }
    fun toggleInputSelected(id: String) { updateInput(id) { it.copy(selected = !it.selected) } }
    fun deleteSelectedInputs() { updateSeason { season -> season.copy(inputs = season.inputs.filterNot { it.selected }) } }

    fun createOutput() {
        val settings = _state.value.settings
        updateSeason { season -> season.copy(outputs = season.outputs + OutputProduct(name = "Temporary ${season.outputs.size + 1}", sellLocation = normalizeLocation(settings.lastSellLocation), sellMethod = normalizeSellMethod(settings.lastSellMethod)))}
    }
    fun updateOutput(id: String, transform: (OutputProduct) -> OutputProduct) { updateSeason { season -> season.copy(outputs = season.outputs.map { if (it.id == id) transform(it) else it }) } }
    fun deleteOutput(id: String) { updateSeason { season -> season.copy(outputs = season.outputs.filter { it.id != id }) } }
    fun duplicateOutput(id: String) { updateSeason { season ->
        val source = season.outputs.firstOrNull { it.id == id } ?: return@updateSeason season
        season.copy(outputs = season.outputs + source.copy(id = java.util.UUID.randomUUID().toString(), name = "${source.name} Copy", selected = false))
    } }
    fun moveOutput(id: String, delta: Int) { updateSeason { season ->
        val list = season.outputs.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (index >= 0 && target >= 0 && index != target) { val item = list.removeAt(index); list.add(target, item) }
        season.copy(outputs = list)
    } }
    fun sortOutputs() { updateSeason { season -> season.copy(outputs = season.outputs.sortedBy { it.name.lowercase() }) } }
    fun toggleOutputSelected(id: String) { updateOutput(id) { it.copy(selected = !it.selected) } }
    fun deleteSelectedOutputs() { updateSeason { season -> season.copy(outputs = season.outputs.filterNot { it.selected }) } }

    fun addAdjustment() { updateSeason { season -> season.copy(adjustments = season.adjustments + Adjustment(name = "Adjustment")) } }
    fun updateAdjustment(id: String, name: String? = null, amount: Double? = null) { updateSeason { season ->
        season.copy(adjustments = season.adjustments.map { if (it.id == id) it.copy(name = name ?: it.name, amount = amount ?: it.amount) else it })
    } }
    fun deleteAdjustment(id: String) { updateSeason { season -> season.copy(adjustments = season.adjustments.filter { it.id != id }) } }

    fun reloadSeason() { updateState { state ->
        val current = currentSeason(state) ?: return@updateState state
        val defaults = if (state.settings.useDefaultSeasonSettings) state.settings.defaultSeason else Season()
        val reset = defaults.copy(id = current.id, temporary = current.temporary, inputs = defaults.inputs.map { it.copy(id = java.util.UUID.randomUUID().toString()) }, outputs = defaults.outputs.map { it.copy(id = java.util.UUID.randomUUID().toString()) })
        state.copy(seasons = state.seasons.map { if (it.id == current.id) reset else it }, undo = (listOf(current) + state.undo).take(30), redo = emptyList())
    } }
    fun saveSelected() { updateState { state ->
        val result = state.calculation
        state.copy(seasons = state.seasons.map { if (it.id == state.selectedSeasonId) it.copy(temporary = false, editedAt = System.currentTimeMillis(), name = if (it.name.isBlank()) "Untitled Season" else it.name, totalExpenses = result?.totalExpenses ?: it.totalExpenses, totalIncome = result?.totalIncome ?: it.totalIncome, profit = result?.profit ?: it.profit, roiPercent = result?.roi ?: it.roiPercent) else it })
    }; viewModelScope.launch(Dispatchers.IO) { repository.save(PersistedState(_state.value.settings, _state.value.seasons, _state.value.openSeasonIds, _state.value.selectedSeasonId)) } }

    fun saveCurrentSeasonAsDefault() { updateState { state ->
        val current = currentSeason(state) ?: return@updateState state
        state.copy(settings = state.settings.copy(useDefaultSeasonSettings = true, defaultSeason = current.copy(id = java.util.UUID.randomUUID().toString())))
    } }

    fun resetDefaultSeasonSettings() { updateState { it.copy(settings = it.settings.copy(useDefaultSeasonSettings = false, defaultSeason = Season())) } }

    fun updateSetting(transform: (AppSettings) -> AppSettings) {
        updateState { state ->
            val settings = transform(state.settings).normalized()
            state.copy(settings = settings)
        }
        if (_state.value.settings.forceKeepAlive) KeepAliveService.start(getApplication()) else KeepAliveService.stop(getApplication())
    }
    fun toggleCollapsed(key: String) { updateState { it.copy(collapsed = it.collapsed + (key to !(it.collapsed[key] ?: false))) } }
    fun setNumberPadVisible(visible: Boolean) { updateState { it.copy(numberPadVisible = visible) } }

    fun export(output: OutputStream) { viewModelScope.launch(Dispatchers.IO) { repository.export(output) } }
    fun import(input: InputStream) { viewModelScope.launch(Dispatchers.IO) {
        val saved = repository.import(input)
        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(settings = saved.settings, seasons = saved.seasons, openSeasonIds = saved.openSeasonIds, selectedSeasonId = saved.selectedSeasonId, screen = "home")
            scheduleSave()
        }
    } }

    private fun scheduleSave() {
        if (!_state.value.settings.autoSave) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1000)
            val state = _state.value
            withContext(Dispatchers.IO) {
                repository.save(PersistedState(state.settings, state.seasons, state.openSeasonIds, state.selectedSeasonId))
            }
        }
    }
}
