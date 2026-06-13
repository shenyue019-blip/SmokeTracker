package com.smoketracker.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smoketracker.app.data.Purchase
import com.smoketracker.app.data.Period
import com.smoketracker.app.data.SmokeEvent
import com.smoketracker.app.data.SmokeKind
import com.smoketracker.app.data.StatsCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: SmokeViewModel, modifier: Modifier = Modifier) {
    val events by vm.events.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()
    val cigarettes by vm.cigarettes.collectAsStateWithLifecycle()
    val cigMap = remember(cigarettes) { cigarettes.associateBy { it.id } }

    var periodIdx by remember { mutableIntStateOf(0) }
    val period = Period.entries[periodIdx]
    val stats = remember(period, events, purchases, cigMap) {
        StatsCalculator.stats(period, events, purchases, cigMap)
    }

    // 三分类：0=抽烟 1=散烟 2=买烟
    var tabSel by remember { mutableIntStateOf(0) }
    var editingSmoke by remember { mutableStateOf<SmokeEvent?>(null) }
    var editingPurchase by remember { mutableStateOf<Purchase?>(null) }

    val smokeList = remember(events) { events.filter { it.kind == SmokeKind.SELF } }
    val shareList = remember(events) {
        events.filter { it.kind == SmokeKind.GIVE || it.kind == SmokeKind.RECEIVE }
    }

    Column(modifier.fillMaxWidth()) {
        ScrollableTabRow(selectedTabIndex = periodIdx, edgePadding = 8.dp) {
            Period.entries.forEachIndexed { i, p ->
                Tab(selected = i == periodIdx, onClick = { periodIdx = i }, text = { Text(p.label) })
            }
        }

        SectionCard("本${period.label}汇总") {
            Spacer(Modifier.height(8.dp))
            TripleRow(
                "${stats.smokeCount}" to "抽烟根数",
                money(stats.smokeCost) to "抽烟花费",
                "${stats.purchaseCount}" to "购入根数"
            )
            Spacer(Modifier.height(12.dp))
            TripleRow(
                mg(stats.tarMg) to "焦油摄入",
                mg(stats.nicotineMg) to "尼古丁摄入",
                money(stats.purchaseCost) to "买烟花费"
            )
            Spacer(Modifier.height(12.dp))
            TripleRow(
                "${stats.giveCount}" to "散给别人(根)",
                money(stats.giveCost) to "散烟花费",
                "${stats.receiveCount}" to "别人给的(根)"
            )
        }

        // 三个独立列表的切换器
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(tabSel == 0, { tabSel = 0 }, { Text("抽烟 (${smokeList.size})") })
            FilterChip(tabSel == 1, { tabSel = 1 }, { Text("散烟 (${shareList.size})") })
            FilterChip(tabSel == 2, { tabSel = 2 }, { Text("买烟 (${purchases.size})") })
        }
        Text(
            "点任意一条可修改或删除",
            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (tabSel) {
            0 -> if (smokeList.isEmpty()) EmptyHint("还没有抽烟记录")
            else LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(smokeList, key = { it.id }) { e ->
                    val price = cigMap[e.cigaretteId]?.pricePerCig ?: e.cost
                    SmokeRow(e, price) { editingSmoke = e }
                }
            }
            1 -> if (shareList.isEmpty()) EmptyHint("还没有散烟记录")
            else LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(shareList, key = { it.id }) { e ->
                    val price = cigMap[e.cigaretteId]?.pricePerCig ?: e.cost
                    SmokeRow(e, price) { editingSmoke = e }
                }
            }
            else -> if (purchases.isEmpty()) EmptyHint("还没有买烟记录")
            else LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(purchases, key = { it.id }) { p ->
                    PurchaseRow(p) { editingPurchase = p }
                }
            }
        }
    }

    editingSmoke?.let { target ->
        EditSmokeDialog(
            event = target,
            cigarettes = cigarettes,
            onDismiss = { editingSmoke = null },
            onSave = { cig, ts -> vm.updateEvent(target, cig, ts); editingSmoke = null },
            onDelete = { vm.deleteEvent(target); editingSmoke = null }
        )
    }
    editingPurchase?.let { target ->
        EditPurchaseDialog(
            purchase = target,
            cigarettes = cigarettes,
            onDismiss = { editingPurchase = null },
            onSave = { cig, packs, cost, ts ->
                vm.updatePurchase(target, cig, packs, cost, ts); editingPurchase = null
            },
            onDelete = { vm.deletePurchase(target); editingPurchase = null }
        )
    }
}

@Composable
private fun SmokeRow(e: SmokeEvent, price: Double, onClick: () -> Unit) {
    val (title, value) = when (e.kind) {
        SmokeKind.GIVE -> "🎁 散给别人 · ${e.cigaretteName}" to money(price)
        SmokeKind.RECEIVE -> "🤝 别人给的 · ${e.cigaretteName}" to "免费"
        else -> "🚬 ${e.cigaretteName}" to money(price)
    }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            KeyValueRow(title, value)
            Text(
                dateTimeOf(e.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PurchaseRow(p: Purchase, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            KeyValueRow("🛒 ${p.cigaretteName}", money(p.totalCost))
            Text(
                "${p.packs} 包（${p.cigsCount} 根） · ${dateTimeOf(p.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
