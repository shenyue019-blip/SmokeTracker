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

/** 把毫秒间隔格式化为冒号分隔的 时:分:秒（单行不换行）。 */
fun durationSince(ts: Long?, now: Long): String {
    if (ts == null) return "—"
    val diff = (now - ts).coerceAtLeast(0)
    val sec = diff / 1000
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, s)
}
