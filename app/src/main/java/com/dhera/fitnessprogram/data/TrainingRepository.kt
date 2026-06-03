package com.dhera.fitnessprogram.data

import com.dhera.fitnessprogram.data.entity.DailyProgress
import com.dhera.fitnessprogram.data.entity.TrainingItem
import com.dhera.fitnessprogram.data.entity.TrainingPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TrainingRepository(private val trainingDao: TrainingDao) {

    val allPlans: Flow<List<TrainingPlan>> = trainingDao.getAllPlans()

    fun getItemsForPlan(planId: Long): Flow<List<TrainingItem>> = trainingDao.getItemsForPlan(planId)

    fun getProgressForDate(date: LocalDate): Flow<List<DailyProgress>> = trainingDao.getProgressForDate(date)

    suspend fun insertPlan(plan: TrainingPlan, items: List<TrainingItem>) {
        val planId = trainingDao.insertPlan(plan)
        items.forEach {
            trainingDao.insertItem(it.copy(planId = planId))
        }
    }

    suspend fun deletePlan(plan: TrainingPlan) {
        trainingDao.deletePlan(plan)
    }

    suspend fun updatePlan(plan: TrainingPlan) {
        trainingDao.updatePlan(plan)
    }

    suspend fun insertItem(item: TrainingItem) {
        trainingDao.insertItem(item)
    }

    suspend fun updateItem(item: TrainingItem) {
        trainingDao.updateItem(item)
    }

    suspend fun updateItems(items: List<TrainingItem>) {
        trainingDao.updateItems(items)
    }

    suspend fun deleteItem(item: TrainingItem) {
        trainingDao.deleteItem(item)
    }

    suspend fun updateProgress(progress: DailyProgress) {
        trainingDao.insertProgress(progress)
    }

    suspend fun getAllPlansWithItems(): List<Pair<TrainingPlan, List<TrainingItem>>> {
        val plans = trainingDao.getAllPlansSync()
        val items = trainingDao.getAllItemsSync()
        return plans.map { plan ->
            plan to items.filter { it.planId == plan.id }
        }
    }
}
