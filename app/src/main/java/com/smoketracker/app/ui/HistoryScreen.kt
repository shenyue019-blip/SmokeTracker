package com.smoketracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smoketracker.app.data.Period
import com.smoketracker.app.data.StatsCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: SmokeViewModel, modifier: Modifier = Modifier) {
    val events by vm.events.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()

    var periodIdx by remember { mutableIntStateOf(0) }
    val period = Period.entries[periodIdx]
    val stats = remember(period, events, purchases) {
        StatsCalculator.stats(period, events, purchases)
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
        }

        // 明细：抽烟 + 买烟 混合，按时间倒序
        val items = remember(period, events, purchases) {
            val from = when (period) {
                Period.ALL -> 0L
                else -> 0L // 明细列出全部，汇总卡片已按周期统计
            }
            val smokeItems = events.map { HistoryItem.Smoke(it.timestamp, it.cigaretteName, it.cost) }
            val buyItems = purchases.map {
                HistoryItem.Buy(it.timestamp, it.cigaretteName, it.packs, it.cigsCount, it.totalCost)
            }
            (smokeItems + buyItems).sortedByDescending { it.timestamp }
        }

        Text(
            "全部明细（${items.size} 条）",
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (items.isEmpty()) {
            EmptyHint("还没有任何记录")
        } else {
            LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(items) { item -> HistoryRow(item) }
            }
        }
    }
}

private sealed interface HistoryItem {
    val timestamp: Long
    data class Smoke(override val timestamp: Long, val name: String, val cost: Double) : HistoryItem
    data class Buy(
        override val timestamp: Long,
        val name: String,
        val packs: Int,
        val cigs: Int,
        val cost: Double
    ) : HistoryItem
}

@Composable
private fun HistoryRow(item: HistoryItem) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            when (item) {
                is HistoryItem.Smoke -> {
                    KeyValueRow("🚬 抽烟 · ${item.name}", money(item.cost))
                    Text(dateTimeOf(item.timestamp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is HistoryItem.Buy -> {
                    KeyValueRow("🛒 买烟 · ${item.name}", money(item.cost))
                    Text(
                        "${item.packs} 包（${item.cigs} 根） · ${dateTimeOf(item.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
