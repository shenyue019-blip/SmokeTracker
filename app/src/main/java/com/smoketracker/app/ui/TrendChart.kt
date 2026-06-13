package com.smoketracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** 近 7 天柱状趋势图（纯 Compose，无第三方依赖）。 */
@Composable
fun TrendChart(data: List<Pair<Long, Int>>) {
    val maxVal = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val barShape: Shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    Row(
        Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (ts, count) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (count > 0) count.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                val fraction = count.toFloat() / maxVal
                Box(
                    Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .width(18.dp)
                        .height((100 * fraction).dp.coerceAtLeast(3.dp))
                        .clip(barShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(weekday(ts), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun weekday(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "一"
        Calendar.TUESDAY -> "二"
        Calendar.WEDNESDAY -> "三"
        Calendar.THURSDAY -> "四"
        Calendar.FRIDAY -> "五"
        Calendar.SATURDAY -> "六"
        else -> "日"
    }
}
