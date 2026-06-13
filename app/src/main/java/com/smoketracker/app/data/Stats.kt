package com.smoketracker.app.data

import java.util.Calendar

enum class Period(val label: String) {
    DAY("日"), WEEK("周"), MONTH("月"), YEAR("年"), ALL("总计")
}

/** 一个时间段内的抽烟 + 买烟汇总。 */
data class PeriodStats(
    val period: Period,
    val smokeCount: Int,
    val smokeCost: Double,
    val tarMg: Double,
    val nicotineMg: Double,
    val purchaseCount: Int,
    val purchaseCost: Double
)

/** 首页今日卡片需要的实时数据。 */
data class TodayStats(
    val count: Int = 0,
    val cost: Double = 0.0,
    val tarMg: Double = 0.0,
    val nicotineMg: Double = 0.0,
    val lastSmokeAt: Long? = null,
    val yesterdayCount: Int = 0,
    val weeklyAverage: Double = 0.0,
    val trackedDays: Int = 0,
    val remainingCigs: Int = 0,           // 库存：买的根数 - 抽的根数
    val totalSmokeCount: Int = 0,
    val totalSmokeCost: Double = 0.0,
    val totalPurchaseCost: Double = 0.0
)

object StatsCalculator {

    private fun startOf(period: Period, now: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return when (period) {
            Period.DAY -> cal.timeInMillis
            Period.WEEK -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.timeInMillis
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            Period.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            Period.ALL -> 0L
        }
    }

    /**
     * 取一次抽烟事件的实际计费数值。
     * 优先用「烟品当前数值」——这样用户后补价格/焦油/尼古丁后，过去的记录会自动算上。
     * 只有当烟品已被删除（map 里查不到）时，才退回事件保存的快照。
     */
    private fun effective(e: SmokeEvent, cigs: Map<Long, Cigarette>): Triple<Double, Double, Double> {
        val c = cigs[e.cigaretteId]
        return if (c != null) Triple(c.pricePerCig, c.tarMg, c.nicotineMg)
        else Triple(e.cost, e.tarMg, e.nicotineMg)
    }

    fun stats(
        period: Period,
        events: List<SmokeEvent>,
        purchases: List<Purchase>,
        cigs: Map<Long, Cigarette>,
        now: Long = System.currentTimeMillis()
    ): PeriodStats {
        val from = startOf(period, now)
        val e = events.filter { it.timestamp >= from }
        val p = purchases.filter { it.timestamp >= from }
        val vals = e.map { effective(it, cigs) }
        return PeriodStats(
            period = period,
            smokeCount = e.size,
            smokeCost = vals.sumOf { it.first },
            tarMg = vals.sumOf { it.second },
            nicotineMg = vals.sumOf { it.third },
            purchaseCount = p.sumOf { it.cigsCount },
            purchaseCost = p.sumOf { it.totalCost }
        )
    }

    fun today(
        events: List<SmokeEvent>,
        purchases: List<Purchase>,
        cigs: Map<Long, Cigarette>,
        now: Long = System.currentTimeMillis()
    ): TodayStats {
        val dayStart = startOf(Period.DAY, now)
        val yesterdayStart = dayStart - 24L * 60 * 60 * 1000
        val todayEvents = events.filter { it.timestamp >= dayStart }
        val yesterdayCount = events.count { it.timestamp in yesterdayStart until dayStart }

        // 已记录天数：有抽烟记录的不同自然日数量。
        val trackedDays = events.map { dayKey(it.timestamp) }.toSet().size
        val totalCount = events.size
        // 本周日均：按已度过的天数平摊（最少 1 天）。
        val weekStart = startOf(Period.WEEK, now)
        val weekCount = events.count { it.timestamp >= weekStart }
        val daysIntoWeek = (((now - weekStart) / (24L * 60 * 60 * 1000)) + 1).toInt().coerceAtLeast(1)
        val weeklyAverage = weekCount.toDouble() / daysIntoWeek

        val boughtCigs = purchases.sumOf { it.cigsCount }
        val remaining = (boughtCigs - totalCount)

        val todayVals = todayEvents.map { effective(it, cigs) }
        return TodayStats(
            count = todayEvents.size,
            cost = todayVals.sumOf { it.first },
            tarMg = todayVals.sumOf { it.second },
            nicotineMg = todayVals.sumOf { it.third },
            lastSmokeAt = events.maxByOrNull { it.timestamp }?.timestamp,
            yesterdayCount = yesterdayCount,
            weeklyAverage = weeklyAverage,
            trackedDays = trackedDays,
            remainingCigs = remaining,
            totalSmokeCount = totalCount,
            totalSmokeCost = events.sumOf { effective(it, cigs).first },
            totalPurchaseCost = purchases.sumOf { it.totalCost }
        )
    }

    /** 近 7 天每天的抽烟根数（含今天），用于趋势图。index 0 = 6 天前。 */
    fun last7Days(events: List<SmokeEvent>, now: Long = System.currentTimeMillis()): List<Pair<Long, Int>> {
        val dayStart = startOf(Period.DAY, now)
        val oneDay = 24L * 60 * 60 * 1000
        return (6 downTo 0).map { offset ->
            val start = dayStart - offset * oneDay
            val end = start + oneDay
            start to events.count { it.timestamp in start until end }
        }
    }

    private fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
