package com.dhera.fitnessprogram.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhera.fitnessprogram.data.TrainingRepository
import com.dhera.fitnessprogram.data.entity.DailyProgress
import com.dhera.fitnessprogram.data.entity.TrainingItem
import com.dhera.fitnessprogram.data.entity.TrainingPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TodayTask(
    val item: TrainingItem,
    val planName: String,
    val completedSets: Int,
    val isFinished: Boolean
)

enum class GlobalTimerType { REST, DURATION }

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application, private val repository: TrainingRepository) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fitness_settings", Context.MODE_PRIVATE)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Music Settings
    private val _playMusicOnStartup = MutableStateFlow(prefs.getBoolean("play_music_on_startup", true))
    val playMusicOnStartup: StateFlow<Boolean> = _playMusicOnStartup.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    // Global Timer State
    private val _activeTimerType = MutableStateFlow<GlobalTimerType?>(null)
    val activeTimerType = _activeTimerType.asStateFlow()
    
    private val _activeTimerTarget = MutableStateFlow(0)
    val activeTimerTarget = _activeTimerTarget.asStateFlow()
    
    private val _activeTimerTask = MutableStateFlow<TodayTask?>(null)
    val activeTimerTask = _activeTimerTask.asStateFlow()
    
    private val _timerMinimized = MutableStateFlow(false)
    val timerMinimized = _timerMinimized.asStateFlow()

    fun startTimer(type: GlobalTimerType, target: Int, task: TodayTask) {
        _activeTimerType.value = type
        _activeTimerTarget.value = target
        _activeTimerTask.value = task
        _timerMinimized.value = false
    }

    fun closeTimer() {
        _activeTimerType.value = null
        _activeTimerTask.value = null
    }

    fun toggleTimerMinimize() {
        _timerMinimized.value = !_timerMinimized.value
    }

    fun setPlayMusicOnStartup(enabled: Boolean) {
        _playMusicOnStartup.value = enabled
        prefs.edit().putBoolean("play_music_on_startup", enabled).apply()
    }

    fun setMusicPlaying(playing: Boolean) {
        _isMusicPlaying.value = playing
    }

    // Today's tasks state
    val todayTasks: StateFlow<List<TodayTask>> = combine(
        repository.allPlans,
        _selectedDate
    ) { plans, date ->
        val activePlans = plans.filter { plan ->
            val daysPassed = ChronoUnit.DAYS.between(plan.startDate, date)
            daysPassed >= 0 && daysPassed % plan.intervalDays == 0L
        }
        activePlans
    }.flatMapLatest { activePlans ->
        val itemFlows = activePlans.map { plan ->
            repository.getItemsForPlan(plan.id).map { items ->
                plan.name to items
            }
        }
        if (itemFlows.isEmpty()) flowOf(emptyList())
        else combine(itemFlows) { planItemsList ->
            planItemsList.flatMap { (planName, items) ->
                items.map { it to planName }
            }
        }
    }.combine(
        _selectedDate.flatMapLatest { repository.getProgressForDate(it) }
    ) { itemsWithPlanName, progressList ->
        itemsWithPlanName.map { (item, planName) ->
            val progress = progressList.find { it.itemId == item.id }
            TodayTask(
                item = item,
                planName = planName,
                completedSets = progress?.completedSets ?: 0,
                isFinished = progress?.isFinished ?: false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun incrementTaskSets(task: TodayTask) {
        viewModelScope.launch {
            val newCompletedSets = task.completedSets + 1
            val isNowFinished = newCompletedSets >= task.item.sets
            repository.updateProgress(
                DailyProgress(
                    date = _selectedDate.value,
                    itemId = task.item.id,
                    completedSets = newCompletedSets,
                    isFinished = isNowFinished || task.isFinished
                )
            )
        }
    }

    fun toggleTaskFinished(task: TodayTask) {
        viewModelScope.launch {
            repository.updateProgress(
                DailyProgress(
                    date = _selectedDate.value,
                    itemId = task.item.id,
                    completedSets = if (!task.isFinished) task.item.sets else 0,
                    isFinished = !task.isFinished
                )
            )
        }
    }

    // Plans management
    val allPlans = repository.allPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPlan(name: String, startDate: LocalDate, intervalDays: Int, items: List<TrainingItem>) {
        viewModelScope.launch {
            repository.insertPlan(
                TrainingPlan(name = name, startDate = startDate, intervalDays = intervalDays),
                items
            )
        }
    }

    fun deletePlan(plan: TrainingPlan) {
        viewModelScope.launch {
            repository.deletePlan(plan)
        }
    }

    fun updatePlan(plan: TrainingPlan) {
        viewModelScope.launch {
            repository.updatePlan(plan)
        }
    }

    fun getItemsForPlan(planId: Long): Flow<List<TrainingItem>> {
        return repository.getItemsForPlan(planId)
    }

    fun addItemToPlan(item: TrainingItem) {
        viewModelScope.launch {
            repository.insertItem(item)
        }
    }

    fun updateItem(item: TrainingItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun updateItemsOrder(items: List<TrainingItem>) {
        viewModelScope.launch {
            val updatedItems = items.mapIndexed { index, item ->
                item.copy(orderIndex = index)
            }
            repository.updateItems(updatedItems)
        }
    }

    fun deleteItem(item: TrainingItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    suspend fun getAllData(): List<Pair<TrainingPlan, List<TrainingItem>>> {
        return repository.getAllPlansWithItems()
    }

    fun importPlans(plansWithItems: List<Pair<TrainingPlan, List<TrainingItem>>>) {
        viewModelScope.launch {
            plansWithItems.forEach { (plan, items) ->
                repository.insertPlan(plan, items)
            }
        }
    }
}

class MainViewModelFactory(private val application: Application, private val repository: TrainingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
