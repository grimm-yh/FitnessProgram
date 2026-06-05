package com.dhera.fitnessprogram.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val intervalDays: Int
)

@Entity(
    tableName = "training_items",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class TrainingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val sets: Int = 1,
    val count: Int? = null,
    val duration: Int? = null, // in seconds
    val rest: Int? = null, // in seconds
    val orderIndex: Int = 0,
    val notes: String = ""
)

@Entity(
    tableName = "daily_progress",
    indices = [Index(value = ["date", "itemId"], unique = true)]
)
data class DailyProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val itemId: Long,
    val completedSets: Int = 0,
    val isFinished: Boolean = false
)
