package com.smoketracker.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateTimeFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

fun money(v: Double): String = "¥" + String.format(Locale.getDefault(), "%.2f", v)
fun mg(v: Double): String = String.format(Locale.getDefault(), "%.1f", v) + " mg"
fun avg(v: Double): String = String.format(Locale.getDefault(), "%.1f", v)

fun timeOf(ts: Long): String = timeFmt.format(Date(ts))
fun dateTimeOf(ts: Long): String = dateTimeFmt.format(Date(ts))

/** 把毫秒间隔格式化为「X小时Y分钟」之类。 */
fun durationSince(ts: Long?, now: Long): String {
    if (ts == null) return "—"
    val diff = (now - ts).coerceAtLeast(0)
    val sec = diff / 1000
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return when {
        h > 0 -> "${h}小时${m}分钟"
        m > 0 -> "${m}分钟${s}秒"
        else -> "${s}秒"
    }
}
