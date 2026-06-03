package com.dhera.fitnessprogram.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dhera.fitnessprogram.data.entity.TrainingItem
import com.dhera.fitnessprogram.data.entity.TrainingPlan
import com.dhera.fitnessprogram.ui.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlansScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val plans by viewModel.allPlans.collectAsState()
    var selectedPlan by remember { mutableStateOf<TrainingPlan?>(null) }
    var showAddPlanDialog by remember { mutableStateOf(false) }

    if (selectedPlan != null) {
        PlanDetailView(
            plan = selectedPlan!!,
            viewModel = viewModel,
            onBack = { selectedPlan = null },
            modifier = modifier
        )
        BackHandler { selectedPlan = null }
    } else {
        Scaffold(
            modifier = modifier,
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddPlanDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新增计划")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Text(text = "训练计划管理", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                if (plans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "暂无计划，请点击右下角添加")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(plans) { _, plan ->
                            PlanItem(
                                plan = plan,
                                onClick = { selectedPlan = plan },
                                onDelete = { viewModel.deletePlan(plan) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPlanDialog) {
        AddEditPlanDialog(
            onDismiss = { showAddPlanDialog = false },
            onConfirm = { name, startDate, interval ->
                viewModel.addPlan(name, startDate, interval, emptyList())
                showAddPlanDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailView(
    plan: TrainingPlan,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dbItems by viewModel.getItemsForPlan(plan.id).collectAsState(initial = emptyList())
    val items = remember { mutableStateListOf<TrainingItem>() }
    
    LaunchedEffect(dbItems) {
        items.clear()
        items.addAll(dbItems)
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TrainingItem?>(null) }
    var editingPlan by remember { mutableStateOf<TrainingPlan?>(null) }

    val lazyListState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(plan.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { editingPlan = plan }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑计划")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加动作")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(text = "开始日期: ${plan.startDate}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "训练周期: 每${plan.intervalDays}天一次", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "训练动作 (长按：左右划删除/编辑，上下拖动排序)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(text = "暂无动作，点击右下角添加", color = MaterialTheme.colorScheme.outline)
            } else {
                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        viewModel.deleteItem(item)
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        editingItem = item
                                        false // Don't dismiss, just trigger edit
                                    }
                                    else -> false
                                }
                            }
                        )

                        val isDragging = index == draggedItemIndex
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                        val zIndex = if (isDragging) 1f else 0f

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.6f)
                                    SwipeToDismissBoxValue.StartToEnd -> Color.Green.copy(alpha = 0.6f)
                                    else -> Color.Transparent
                                }
                                val icon = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                    else -> null
                                }
                                Box(
                                    Modifier.fillMaxSize().background(color).padding(horizontal = 16.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    icon?.let { Icon(it, contentDescription = null, tint = Color.White) }
                                }
                            },
                            modifier = Modifier
                                .zIndex(zIndex)
                                .offset(y = if (isDragging) draggingOffset.dp else 0.dp)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedItemIndex = index },
                                        onDragEnd = {
                                            draggedItemIndex = null
                                            draggingOffset = 0f
                                            viewModel.updateItemsOrder(items.toList())
                                        },
                                        onDragCancel = {
                                            draggedItemIndex = null
                                            draggingOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingOffset += dragAmount.y / density
                                            
                                            // Simple reordering logic
                                            val currentDraggingIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                            val targetIndex = if (draggingOffset > 50 && currentDraggingIndex < items.size - 1) {
                                                currentDraggingIndex + 1
                                            } else if (draggingOffset < -50 && currentDraggingIndex > 0) {
                                                currentDraggingIndex - 1
                                            } else {
                                                null
                                            }
                                            
                                            if (targetIndex != null) {
                                                val movedItem = items.removeAt(currentDraggingIndex)
                                                items.add(targetIndex, movedItem)
                                                draggedItemIndex = targetIndex
                                                draggingOffset = 0f
                                            }
                                        }
                                    )
                                }
                                .animateItem()
                        ) {
                            TrainingItemRow(item, elevation = elevation)
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddEditItemDialog(
            planId = plan.id,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { newItem ->
                viewModel.addItemToPlan(newItem.copy(orderIndex = items.size))
                showAddItemDialog = false
            }
        )
    }

    if (editingItem != null) {
        AddEditItemDialog(
            planId = plan.id,
            item = editingItem,
            onDismiss = { editingItem = null },
            onConfirm = { updatedItem ->
                viewModel.updateItem(updatedItem)
                editingItem = null
            }
        )
    }
    
    if (editingPlan != null) {
        AddEditPlanDialog(
            plan = editingPlan,
            onDismiss = { editingPlan = null },
            onConfirm = { name, startDate, interval ->
                viewModel.updatePlan(editingPlan!!.copy(name = name, startDate = startDate, intervalDays = interval))
                editingPlan = null
            }
        )
    }
}

@Composable
fun PlanItem(plan: TrainingPlan, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = plan.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "周期: ${plan.intervalDays}天", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

@Composable
fun TrainingItemRow(item: TrainingItem, elevation: androidx.compose.ui.unit.Dp = 0.dp) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${item.sets}组 | ${item.count ?: "-"}次 | ${item.duration ?: "-"}秒 | 间歇${item.rest ?: "-"}秒",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlanDialog(
    plan: TrainingPlan? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, Int) -> Unit
) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var interval by remember { mutableStateOf(plan?.intervalDays?.toString() ?: "1") }
    var startDate by remember { mutableStateOf(plan?.startDate ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "新增计划" else "编辑计划") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("计划名称") })
                TextField(value = interval, onValueChange = { interval = it }, label = { Text("周期（天）") })
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "开始时间", style = MaterialTheme.typography.labelSmall)
                            Text(text = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, startDate, interval.toIntOrNull() ?: 1) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AddEditItemDialog(
    planId: Long,
    item: TrainingItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (TrainingItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var sets by remember { mutableStateOf(item?.sets?.toString() ?: "1") }
    var count by remember { mutableStateOf(item?.count?.toString() ?: "") }
    var duration by remember { mutableStateOf(item?.duration?.toString() ?: "") }
    var rest by remember { mutableStateOf(item?.rest?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "添加动作" else "编辑动作") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TextField(value = name, onValueChange = { name = it }, label = { Text("动作名称") }) }
                item { TextField(value = sets, onValueChange = { sets = it }, label = { Text("组数") }) }
                item { TextField(value = count, onValueChange = { count = it }, label = { Text("次数 (可选)") }) }
                item { TextField(value = duration, onValueChange = { duration = it }, label = { Text("时长/秒 (可选)") }) }
                item { TextField(value = rest, onValueChange = { rest = it }, label = { Text("间歇/秒 (可选)") }) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    TrainingItem(
                        id = item?.id ?: 0,
                        planId = planId,
                        name = name,
                        sets = sets.toIntOrNull() ?: 1,
                        count = count.toIntOrNull(),
                        duration = duration.toIntOrNull(),
                        rest = rest.toIntOrNull(),
                        orderIndex = item?.orderIndex ?: 0
                    )
                )
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
