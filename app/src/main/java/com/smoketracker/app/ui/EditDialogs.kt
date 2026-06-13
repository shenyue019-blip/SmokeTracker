package com.smoketracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smoketracker.app.data.Cigarette
import com.smoketracker.app.data.Purchase
import com.smoketracker.app.data.SmokeEvent
import java.util.Calendar
import java.util.TimeZone

/** 把 DatePicker（UTC 零点）选的日期与 TimePicker 选的时分，合成本地时区时间戳。 */
private fun combineDateTime(dateUtcMillis: Long, hour: Int, minute: Int): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
    }.timeInMillis
}

/** 点击后先选日期、再选时间，回填到 onChange。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(millis: Long, onChange: (Long) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pendingDateUtc by remember { mutableLongStateOf(millis) }

    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
        Text("时间：" + dateTimeOf(millis))
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateUtc = state.selectedDateMillis ?: millis
                    showDate = false
                    showTime = true
                }) { Text("下一步：选时间") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val tState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(combineDateTime(pendingDateUtc, tState.hour, tState.minute))
                    showTime = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } },
            text = { TimePicker(state = tState) }
        )
    }
}

@Composable
fun EditSmokeDialog(
    event: SmokeEvent,
    cigarettes: List<Cigarette>,
    onDismiss: () -> Unit,
    onSave: (Cigarette, Long) -> Unit,
    onDelete: () -> Unit
) {
    var selected by remember {
        mutableStateOf(cigarettes.firstOrNull { it.id == event.cigaretteId } ?: cigarettes.firstOrNull())
    }
    var ts by remember { mutableLongStateOf(event.timestamp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑抽烟记录") },
        text = {
            Column {
                CigaretteDropdown("烟品", cigarettes, selected) { selected = it }
                Spacer(Modifier.height(12.dp))
                DateTimeField(ts) { ts = it }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete) {
                    Text("删除这条记录", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected != null, onClick = { selected?.let { onSave(it, ts) } }) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun EditPurchaseDialog(
    purchase: Purchase,
    cigarettes: List<Cigarette>,
    onDismiss: () -> Unit,
    onSave: (cig: Cigarette, packs: Int, totalCost: Double, timestamp: Long) -> Unit,
    onDelete: () -> Unit
) {
    var selected by remember {
        mutableStateOf(cigarettes.firstOrNull { it.id == purchase.cigaretteId } ?: cigarettes.firstOrNull())
    }
    var packs by remember { mutableStateOf(purchase.packs.toString()) }
    var cost by remember { mutableStateOf(if (purchase.totalCost % 1.0 == 0.0) purchase.totalCost.toInt().toString() else purchase.totalCost.toString()) }
    var ts by remember { mutableLongStateOf(purchase.timestamp) }

    val valid = selected != null && (packs.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑买烟记录") },
        text = {
            Column {
                CigaretteDropdown("烟品", cigarettes, selected) { selected = it }
                OutlinedTextField(
                    value = packs, onValueChange = { packs = it },
                    label = { Text("买几包") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = cost, onValueChange = { cost = it },
                    label = { Text("总花费（元）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                DateTimeField(ts) { ts = it }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete) {
                    Text("删除这条记录", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    selected?.let {
                        onSave(it, packs.toIntOrNull() ?: 1, cost.toDoubleOrNull() ?: 0.0, ts)
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
