package com.dhera.fitnessprogram.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dhera.fitnessprogram.MusicManager
import com.dhera.fitnessprogram.data.entity.TrainingItem
import com.dhera.fitnessprogram.data.entity.TrainingPlan
import com.dhera.fitnessprogram.ui.MainViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

@Composable
fun SettingsScreen(
    viewModel: MainViewModel, 
    musicManager: MusicManager, 
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playMusicOnStartup by viewModel.playMusicOnStartup.collectAsState()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let { 
                scope.launch {
                    exportData(context, it, viewModel)
                }
            }
        }
    )
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    importData(context, it, viewModel)
                }
            }
        }
    )

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Music Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "启动时播放音乐", style = MaterialTheme.typography.titleMedium)
                    Text(text = "打开App时自动播放背景音乐", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = playMusicOnStartup,
                    onCheckedChange = { 
                        musicManager.stopAllNotifications()
                        viewModel.setPlayMusicOnStartup(it)
                        if (it) {
                            musicManager.startBackgroundMusic()
                            viewModel.setMusicPlaying(true)
                        } else {
                            musicManager.pauseBackgroundMusic()
                            viewModel.setMusicPlaying(false)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                musicManager.stopAllNotifications()
                exportLauncher.launch("fitness_plans.json") 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("导出 JSON")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                musicManager.stopAllNotifications()
                importLauncher.launch(arrayOf("application/json")) 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("导入 JSON")
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                musicManager.stopAllNotifications()
                onAboutClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("关于")
        }
    }
}

// JSON Data Models
data class JsonExport(val plans: List<JsonPlan>)
data class JsonPlan(
    val name: String,
    val startDate: String,
    val interval: Int,
    val items: List<JsonItem>
)
data class JsonItem(
    val name: String,
    val sets: Int,
    val count: Int?,
    val duration: Int?,
    val rest: Int?,
    val notes: String? = ""
)

private suspend fun exportData(context: Context, uri: Uri, viewModel: MainViewModel) {
    withContext(Dispatchers.IO) {
        try {
            val allData = viewModel.getAllData()
            val jsonExport = JsonExport(plans = allData.map { (plan, items) ->
                JsonPlan(
                    name = plan.name,
                    startDate = plan.startDate.toString(),
                    interval = plan.intervalDays,
                    items = items.map { JsonItem(it.name, it.sets, it.count, it.duration, it.rest, it.notes) }
                )
            })
            
            val gson = GsonBuilder().setPrettyPrinting().create()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    gson.toJson(jsonExport, writer)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private suspend fun importData(context: Context, uri: Uri, viewModel: MainViewModel) {
    withContext(Dispatchers.IO) {
        try {
            val gson = GsonBuilder().create()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val jsonExport = gson.fromJson(reader, JsonExport::class.java)
                    val plansWithItems = jsonExport.plans.map { jsonPlan ->
                        val plan = TrainingPlan(
                            name = jsonPlan.name,
                            startDate = LocalDate.parse(jsonPlan.startDate),
                            intervalDays = jsonPlan.interval
                        )
                        val items = jsonPlan.items.map { jsonItem ->
                            TrainingItem(
                                planId = 0, // Will be set by repository
                                name = jsonItem.name,
                                sets = jsonItem.sets,
                                count = jsonItem.count,
                                duration = jsonItem.duration,
                                rest = jsonItem.rest,
                                notes = jsonItem.notes ?: ""
                            )
                        }
                        plan to items
                    }
                    viewModel.importPlans(plansWithItems)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
