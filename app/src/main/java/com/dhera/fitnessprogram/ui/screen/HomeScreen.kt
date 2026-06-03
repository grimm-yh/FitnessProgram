package com.dhera.fitnessprogram.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhera.fitnessprogram.MusicManager
import com.dhera.fitnessprogram.ui.MainViewModel
import com.dhera.fitnessprogram.ui.TodayTask
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun HomeScreen(viewModel: MainViewModel, musicManager: MusicManager, modifier: Modifier = Modifier) {
    val tasks by viewModel.todayTasks.collectAsState()
    val date = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINESE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = date.format(dateFormatter),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = date.format(dayOfWeekFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "今日休息", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            val completedCount = tasks.count { it.completed }
            val progress = completedCount.toFloat() / tasks.size

            Text(text = "今日训练完成率: ${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks) { task ->
                    TaskItem(task, musicManager) {
                        viewModel.toggleTaskCompletion(task)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: TodayTask, musicManager: MusicManager, onToggle: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = buildString {
                            append("${task.item.sets}组 ")
                            task.item.count?.let { append("${it}次 ") }
                            task.item.duration?.let { append("${it}秒 ") }
                            task.item.rest?.let { append("间歇${it}秒") }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "计划: ${task.planName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.completed,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.clickable(enabled = false) { }
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收起" else "展开"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if ((task.item.duration ?: 0) > 0) {
                        DurationTimer(targetSeconds = task.item.duration ?: 0, musicManager = musicManager)
                    }
                    
                    if ((task.item.rest ?: 0) > 0) {
                        if ((task.item.duration ?: 0) > 0) HorizontalDivider()
                        CountdownTimer(durationSeconds = task.item.rest ?: 0, musicManager = musicManager)
                    }
                    
                    if ((task.item.duration ?: 0) <= 0 && (task.item.rest ?: 0) <= 0) {
                        Text(text = "无计时或间歇项", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun DurationTimer(targetSeconds: Int, musicManager: MusicManager) {
    var timeElapsed by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var hasSoundPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            timeElapsed += 1
            if (timeElapsed == targetSeconds && !hasSoundPlayed) {
                musicManager.playTaskFinishSound()
                hasSoundPlayed = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "动作计时: $timeElapsed / $targetSeconds 秒",
            style = MaterialTheme.typography.headlineSmall,
            color = if (timeElapsed >= targetSeconds) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "停止" else "开始"
                )
                Text(text = if (isRunning) "停止" else "开始计时")
            }
            OutlinedButton(onClick = {
                timeElapsed = 0
                isRunning = false
                hasSoundPlayed = false
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "重置")
                Spacer(Modifier.width(4.dp))
                Text(text = "重置")
            }
        }
    }
}

@Composable
fun CountdownTimer(durationSeconds: Int, musicManager: MusicManager) {
    var timeLeft by remember { mutableIntStateOf(durationSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        } else if (timeLeft == 0 && isRunning) {
            isRunning = false
            musicManager.playThreeStageNotification()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "间歇倒计时: $timeLeft 秒",
            style = MaterialTheme.typography.headlineSmall,
            color = if (timeLeft == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "停止" else "开始"
                )
                Text(text = if (isRunning) "停止" else "开始休息")
            }
            OutlinedButton(onClick = {
                timeLeft = durationSeconds
                isRunning = false
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "重置")
                Spacer(Modifier.width(4.dp))
                Text(text = "重置")
            }
        }
    }
}
