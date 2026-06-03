package com.dhera.fitnessprogram.data

import androidx.room.*
import com.dhera.fitnessprogram.data.entity.DailyProgress
import com.dhera.fitnessprogram.data.entity.TrainingItem
import com.dhera.fitnessprogram.data.entity.TrainingPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TrainingDao {

    // Plans
    @Query("SELECT * FROM training_plans")
    fun getAllPlans(): Flow<List<TrainingPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TrainingPlan): Long

    @Delete
    suspend fun deletePlan(plan: TrainingPlan)

    @Update
    suspend fun updatePlan(plan: TrainingPlan)

    // Items
    @Query("SELECT * FROM training_items WHERE planId = :planId ORDER BY orderIndex ASC")
    fun getItemsForPlan(planId: Long): Flow<List<TrainingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TrainingItem): Long

    @Update
    suspend fun updateItem(item: TrainingItem)

    @Update
    suspend fun updateItems(items: List<TrainingItem>)

    @Delete
    suspend fun deleteItem(item: TrainingItem)

    // Progress
    @Query("SELECT * FROM daily_progress WHERE date = :date")
    fun getProgressForDate(date: LocalDate): Flow<List<DailyProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DailyProgress)

    @Query("SELECT * FROM training_plans")
    suspend fun getAllPlansSync(): List<TrainingPlan>

    @Query("SELECT * FROM training_items")
    suspend fun getAllItemsSync(): List<TrainingItem>
}
