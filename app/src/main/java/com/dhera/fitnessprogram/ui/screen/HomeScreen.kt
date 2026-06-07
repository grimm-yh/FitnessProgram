package com.dhera.fitnessprogram.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhera.fitnessprogram.MusicManager
import com.dhera.fitnessprogram.ui.GlobalTimerType
import com.dhera.fitnessprogram.ui.MainViewModel
import com.dhera.fitnessprogram.ui.TodayTask
import kotlinx.coroutines.flow.MutableStateFlow
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
        Text(text = date.format(dateFormatter), style = MaterialTheme.typography.titleLarge)
        Text(text = date.format(dayOfWeekFormatter), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(24.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "今日休息", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            val overallProgress = tasks.count { it.isFinished }.toFloat() / tasks.size
            Text(text = "今日训练完成率: ${(overallProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { overallProgress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks) { task ->
                    TaskItem(
                        task = task,
                        musicManager = musicManager,
                        onToggleFinished = { 
                            musicManager.stopAllNotifications()
                            viewModel.toggleTaskFinished(task) 
                        },
                        onStartTimer = { type, target ->
                            musicManager.stopAllNotifications()
                            viewModel.startTimer(type, target, task, musicManager)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: TodayTask,
    musicManager: MusicManager,
    onToggleFinished: () -> Unit,
    onStartTimer: (GlobalTimerType, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { 
                musicManager.stopAllNotifications()
                expanded = !expanded 
            },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (task.isFinished) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
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
                    val setProgress = if (task.item.sets > 0) task.completedSets.toFloat() / task.item.sets else 1f
                    LinearProgressIndicator(
                        progress = { setProgress },
                        modifier = Modifier.width(100.dp).height(4.dp).padding(vertical = 2.dp),
                        color = if (task.isFinished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${task.completedSets}/${task.item.sets}组 | ${task.item.count ?: "-"}次",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFinished) {
                        Icon(
                            imageVector = if (task.isFinished) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "完成",
                            tint = if (task.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (task.item.notes.isNotEmpty()) {
                        Text(text = "备注: ${task.item.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if ((task.item.duration ?: 0) > 0) {
                            Button(
                                onClick = { onStartTimer(GlobalTimerType.DURATION, task.item.duration!!) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("持续 ${task.item.duration}s", fontSize = 15.sp)
                            }
                        }
                        if ((task.item.rest ?: 0) > 0) {
                            Button(
                                onClick = { onStartTimer(GlobalTimerType.REST, task.item.rest!!) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RectangleShape
                            ) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("间歇 ${task.item.rest}s", fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerWindow(
    type: GlobalTimerType,
    targetSeconds: Int,
    minimized: Boolean,
    onToggleMinimize: () -> Unit,
    onClose: () -> Unit,
    musicManager: MusicManager,
    viewModel: MainViewModel? = null
) {
    val time by (viewModel?.timerCurrentValue ?: MutableStateFlow(0)).collectAsState()
    val isRunning by (viewModel?.timerIsRunning ?: MutableStateFlow(false)).collectAsState()

    val windowModifier = if (minimized) {
        Modifier
            .padding(top = 16.dp, end = 16.dp) // Exactly aligns with the 16dp top padding of the HomeScreen content
            .size(width = 80.dp, height = 64.dp) // Doubled height as requested
            .background(MaterialTheme.colorScheme.secondaryContainer, RectangleShape)
            .clickable { 
                musicManager.stopAllNotifications()
                onToggleMinimize() 
            }
    } else {
        Modifier
            .padding(32.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
            .padding(16.dp)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = if (minimized) Alignment.TopEnd else Alignment.Center) {
        if (!minimized) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { 
                musicManager.stopAllNotifications()
                onToggleMinimize() 
            })
        }
        
        Column(
            modifier = windowModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (minimized) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(if (type == GlobalTimerType.REST) Icons.Default.History else Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "$time", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        musicManager.stopAllNotifications()
                        onToggleMinimize()
                    }) { 
                        Icon(Icons.Default.OpenInFull, contentDescription = "缩小", modifier = Modifier.size(20.dp)) 
                    }
                    Text(text = if (type == GlobalTimerType.REST) "间歇倒计时" else "动作计时", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        musicManager.stopAllNotifications()
                        onClose()
                    }) { 
                        Icon(Icons.Default.Close, contentDescription = "关闭") 
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (type == GlobalTimerType.REST) "$time" else "$time / $targetSeconds",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (type == GlobalTimerType.REST && time == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { 
                            musicManager.stopAllNotifications()
                            viewModel?.pauseResumeTimer(musicManager)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RectangleShape
                    ) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRunning) "暂停" else "继续")
                    }
                    OutlinedButton(
                        onClick = { 
                            musicManager.stopAllNotifications()
                            viewModel?.resetTimer()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RectangleShape
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("重置")
                    }
                }
            }
        }
    }
}
