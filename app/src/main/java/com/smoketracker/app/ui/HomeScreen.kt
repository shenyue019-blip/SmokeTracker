package com.smoketracker.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smoketracker.app.data.StatsCalculator
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: SmokeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cigarettes by vm.cigarettes.collectAsStateWithLifecycle()
    val defaultCig by vm.defaultCigarette.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val purchases by vm.purchases.collectAsStateWithLifecycle()

    // 每秒滴答，用于"距上次抽烟"实时刷新。
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val today = remember(events, purchases, now) { StatsCalculator.today(events, purchases, now) }
    val trend = remember(events, now) { StatsCalculator.last7Days(events, now) }

    var showAdd by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 当前烟品切换
        SectionCard("当前烟品（大按钮记这一种）") {
            if (cigarettes.isEmpty()) {
                EmptyHint("还没有烟品，点下方「新增烟品」先添加一种。")
            } else {
                Spacer(Modifier.height(8.dp))
                CigaretteDropdown("默认烟品", cigarettes, defaultCig) { vm.setDefault(it) }
            }
        }

        // 大按钮
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Button(
                onClick = {
                    defaultCig?.let {
                        vm.smokeOne(it)
                        Toast.makeText(context, "已记录一根 ${it.name}", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = defaultCig != null,
                shape = CircleShape,
                modifier = Modifier.size(180.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("抽一根", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("点我 +1", fontSize = 14.sp)
                }
            }
        }

        // 撤销 / 买烟 / 新增
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    vm.undoLast { ok ->
                        val msg = if (ok) "已撤销上一根" else "没有可撤销的记录"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Undo, null); Spacer(Modifier.size(4.dp)); Text("撤销")
            }
            OutlinedButton(onClick = { showPurchase = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.size(4.dp)); Text("买烟")
            }
            OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.size(4.dp)); Text("烟品")
            }
        }

        // 今日卡片
        SectionCard("今日") {
            Spacer(Modifier.height(8.dp))
            TripleRow(
                "${today.count}" to "今日根数",
                durationSince(today.lastSmokeAt, now) to "距上次",
                money(today.cost) to "今日花费"
            )
            Spacer(Modifier.height(12.dp))
            TripleRow(
                mg(today.tarMg) to "今日焦油",
                mg(today.nicotineMg) to "今日尼古丁",
                "${today.remainingCigs}" to "剩余库存(根)"
            )
            if (today.remainingCigs in 1..5) {
                Spacer(Modifier.height(8.dp))
                Text("⚠ 库存只剩 ${today.remainingCigs} 根，记得补货。", color = MaterialTheme.colorScheme.error)
            } else if (today.remainingCigs <= 0 && purchases.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("⚠ 按记录库存已抽完。", color = MaterialTheme.colorScheme.error)
            }
        }

        // 激励卡片
        MotivationCard(today)

        // 趋势图
        SectionCard("近 7 天趋势") {
            if (events.isEmpty()) EmptyHint("还没有记录") else TrendChart(trend)
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
    if (showPurchase) {
        PurchaseDialog(
            cigarettes = cigarettes,
            defaultCig = defaultCig,
            onDismiss = { showPurchase = false },
            onConfirm = { cig, packs, cost ->
                vm.recordPurchase(cig, packs, cost)
                showPurchase = false
            }
        )
    }
}

@Composable
private fun MotivationCard(today: com.smoketracker.app.data.TodayStats) {
    val diff = today.count - today.yesterdayCount
    val (msg, color) = when {
        today.yesterdayCount == 0 && today.count == 0 -> "新的一天，加油保持！" to Color(0xFF2E7D32)
        diff < 0 -> "比昨天少抽 ${-diff} 根，做得好 👍" to Color(0xFF2E7D32)
        diff == 0 -> "和昨天持平（$diff），试试再少一根？" to Color(0xFFF57C00)
        else -> "比昨天多抽 $diff 根，注意控制 💪" to Color(0xFFC62828)
    }
    SectionCard("坚持与对比") {
        Spacer(Modifier.height(8.dp))
        Text(msg, color = color, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        TripleRow(
            "${today.yesterdayCount}" to "昨日根数",
            avg(today.weeklyAverage) to "本周日均",
            "${today.trackedDays}" to "已记录天数"
        )
    }
}
