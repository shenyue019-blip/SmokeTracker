package com.smoketracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CigarettesScreen(vm: SmokeViewModel, modifier: Modifier = Modifier) {
    val cigarettes by vm.cigarettes.collectAsStateWithLifecycle()
    val defaultCig by vm.defaultCigarette.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("烟品库", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showAdd = true }) { Text("新增烟品") }
        }

        if (cigarettes.isEmpty()) {
            EmptyHint("还没有烟品。点右上角「新增烟品」添加，第一支会自动设为默认。")
        } else {
            LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(cigarettes) { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                if (c.id == defaultCig?.id) {
                                    AssistChip(onClick = {}, label = { Text("默认") })
                                } else {
                                    TextButton(onClick = { vm.setDefault(c) }) { Text("设为默认") }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            KeyValueRow("价格", "${money(c.packPrice)}/包 · ${money(c.pricePerCig)}/根")
                            KeyValueRow("每包支数", "${c.cigsPerPack} 支")
                            KeyValueRow("单根焦油 / 尼古丁", "${avg(c.tarMg)} / ${avg(c.nicotineMg)} mg")
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddCigaretteDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, price, perPack, tar, nic ->
                vm.addCigarette(name, price, perPack, tar, nic)
                showAdd = false
            }
        )
    }
}
