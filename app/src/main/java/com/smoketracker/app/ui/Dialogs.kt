package com.smoketracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smoketracker.app.data.Cigarette

/** 通用：烟品选择下拉框。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CigaretteDropdown(
    label: String,
    cigarettes: List<Cigarette>,
    selected: Cigarette?,
    onSelect: (Cigarette) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.name}（${money(it.pricePerCig)}/根）" } ?: "请选择",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cigarettes.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.name}  ·  ${money(c.packPrice)}/包  ·  焦油${avg(c.tarMg)} 尼古丁${avg(c.nicotineMg)}") },
                    onClick = { onSelect(c); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AddCigaretteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, packPrice: Double, cigsPerPack: Int, tar: Double, nicotine: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var perPack by remember { mutableStateOf("20") }
    var tar by remember { mutableStateOf("") }
    var nicotine by remember { mutableStateOf("") }

    val valid = name.isNotBlank() &&
        price.toDoubleOrNull() != null &&
        (perPack.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增烟品") },
        text = {
            Column {
                NumberOrTextField("烟名", name, KeyboardType.Text) { name = it }
                NumberOrTextField("每包价格（元）", price, KeyboardType.Decimal) { price = it }
                NumberOrTextField("每包支数", perPack, KeyboardType.Number) { perPack = it }
                NumberOrTextField("单根焦油量（mg）", tar, KeyboardType.Decimal) { tar = it }
                NumberOrTextField("单根尼古丁量（mg）", nicotine, KeyboardType.Decimal) { nicotine = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        name,
                        price.toDoubleOrNull() ?: 0.0,
                        perPack.toIntOrNull() ?: 20,
                        tar.toDoubleOrNull() ?: 0.0,
                        nicotine.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun PurchaseDialog(
    cigarettes: List<Cigarette>,
    defaultCig: Cigarette?,
    onDismiss: () -> Unit,
    onConfirm: (cig: Cigarette, packs: Int, totalCost: Double) -> Unit
) {
    var selected by remember { mutableStateOf(defaultCig ?: cigarettes.firstOrNull()) }
    var packs by remember { mutableStateOf("1") }
    var cost by remember { mutableStateOf("") }

    // 价格联动：用户没手动填总价时，按 包数 * 包价 自动估算并展示。
    val autoCost = (packs.toIntOrNull() ?: 0) * (selected?.packPrice ?: 0.0)
    val effectiveCost = cost.toDoubleOrNull() ?: autoCost
    val valid = selected != null && (packs.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录买烟") },
        text = {
            Column {
                CigaretteDropdown("烟品", cigarettes, selected) { selected = it }
                NumberOrTextField("买几包", packs, KeyboardType.Number) { packs = it }
                NumberOrTextField(
                    "总花费（元，留空按 ${money(autoCost)} 计）",
                    cost,
                    KeyboardType.Decimal
                ) { cost = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { selected?.let { onConfirm(it, packs.toIntOrNull() ?: 1, effectiveCost) } }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NumberOrTextField(
    label: String,
    value: String,
    keyboard: KeyboardType,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
