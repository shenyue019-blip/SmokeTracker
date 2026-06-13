package com.smoketracker.app.data

import java.util.Calendar

enum class Period(val label: String) {
    DAY("日"), WEEK("周"), MONTH("月"), YEAR("年"), ALL("总计")
}

/** 一个时间段内的抽烟 + 买烟 + 散烟汇总。 */
data class PeriodStats(
    val period: Period,
    val smokeCount: Int,        // 自抽 + 散入（你实际抽的）
    val smokeCost: Double,      // 自抽花费
    val tarMg: Double,
    val nicotineMg: Double,
    val purchaseCount: Int,
    val purchaseCost: Double,
    val giveCount: Int = 0,     // 散给别人的根数
    val giveCost: Double = 0.0, // 散给别人花的钱
    val receiveCount: Int = 0   // 别人给你的根数
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
    val remainingCigs: Int = 0,           // 库存：买的根数 -（自抽 + 散出）
    val totalSmokeCount: Int = 0,
    val totalSmokeCost: Double = 0.0,
    val totalPurchaseCost: Double = 0.0,
    val giveCount: Int = 0,
    val giveCost: Double = 0.0
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

    /** 一条事件对各项统计的贡献（类型感知）。 */
    private data class Contrib(
        val isSmoke: Boolean,        // 计入根数/焦油/尼古丁（自抽、散入）
        val smokeCost: Double,       // 自抽花费（仅自抽）
        val tar: Double,
        val nic: Double,
        val isGive: Boolean,
        val giveCost: Double,        // 散出花费
        val consumesStock: Boolean   // 减库存（自抽、散出）
    )

    /** 烟品仍在则用其当前数值，已删除则退回事件快照；再按类型路由。 */
    private fun contrib(e: SmokeEvent, cigs: Map<Long, Cigarette>): Contrib {
        val c = cigs[e.cigaretteId]
        val price = c?.pricePerCig ?: e.cost
        val tar = c?.tarMg ?: e.tarMg
        val nic = c?.nicotineMg ?: e.nicotineMg
        return when (e.kind) {
            SmokeKind.GIVE -> Contrib(false, 0.0, 0.0, 0.0, true, price, true)
            SmokeKind.RECEIVE -> Contrib(true, 0.0, tar, nic, false, 0.0, false)
            else -> Contrib(true, price, tar, nic, false, 0.0, true)
        }
    }

    private fun isSmoke(e: SmokeEvent) = e.kind != SmokeKind.GIVE

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
        val c = e.map { contrib(it, cigs) }
        return PeriodStats(
            period = period,
            smokeCount = c.count { it.isSmoke },
            smokeCost = c.sumOf { it.smokeCost },
            tarMg = c.sumOf { it.tar },
            nicotineMg = c.sumOf { it.nic },
            purchaseCount = p.sumOf { it.cigsCount },
            purchaseCost = p.sumOf { it.totalCost },
            giveCount = c.count { it.isGive },
            giveCost = c.sumOf { it.giveCost },
            receiveCount = e.count { it.kind == SmokeKind.RECEIVE }
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
        val todayContrib = todayEvents.map { contrib(it, cigs) }
        // 对比/均值/记录天数都只看「你实际抽的」(自抽+散入)
        val smokes = events.filter { isSmoke(it) }
        val yesterdayCount = smokes.count { it.timestamp in yesterdayStart until dayStart }
        val trackedDays = smokes.map { dayKey(it.timestamp) }.toSet().size

        val weekStart = startOf(Period.WEEK, now)
        val weekCount = smokes.count { it.timestamp >= weekStart }
        val daysIntoWeek = (((now - weekStart) / (24L * 60 * 60 * 1000)) + 1).toInt().coerceAtLeast(1)
        val weeklyAverage = weekCount.toDouble() / daysIntoWeek

        val boughtCigs = purchases.sumOf { it.cigsCount }
        // 库存只被「自抽 + 散出」消耗，散入不减库存
        val stockConsumed = events.count { it.kind == SmokeKind.SELF || it.kind == SmokeKind.GIVE }
        val remaining = boughtCigs - stockConsumed

        val allContrib = events.map { contrib(it, cigs) }
        return TodayStats(
            count = todayContrib.count { it.isSmoke },
            cost = todayContrib.sumOf { it.smokeCost },
            tarMg = todayContrib.sumOf { it.tar },
            nicotineMg = todayContrib.sumOf { it.nic },
            lastSmokeAt = smokes.maxByOrNull { it.timestamp }?.timestamp,
            yesterdayCount = yesterdayCount,
            weeklyAverage = weeklyAverage,
            trackedDays = trackedDays,
            remainingCigs = remaining,
            totalSmokeCount = allContrib.count { it.isSmoke },
            totalSmokeCost = allContrib.sumOf { it.smokeCost },
            totalPurchaseCost = purchases.sumOf { it.totalCost },
            giveCount = todayContrib.count { it.isGive },
            giveCost = todayContrib.sumOf { it.giveCost }
        )
    }

    /** 近 7 天每天的抽烟根数（自抽+散入，含今天），用于趋势图。index 0 = 6 天前。 */
    fun last7Days(events: List<SmokeEvent>, now: Long = System.currentTimeMillis()): List<Pair<Long, Int>> {
        val dayStart = startOf(Period.DAY, now)
        val oneDay = 24L * 60 * 60 * 1000
        val smokes = events.filter { isSmoke(it) }
        return (6 downTo 0).map { offset ->
            val start = dayStart - offset * oneDay
            val end = start + oneDay
            start to smokes.count { it.timestamp in start until end }
        }
    }

    private fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
