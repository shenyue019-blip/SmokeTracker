package com.smoketracker.app.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * 烟品。价格按「包」存储：packPrice + cigsPerPack 自动推导单根价格，
 * 这样抽烟（按根扣费）和买烟（按包付费）口径一致。
 */
@Entity(tableName = "cigarettes")
data class Cigarette(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packPrice: Double,      // 每包价格（元）
    val cigsPerPack: Int,       // 每包支数，通常 20
    val tarMg: Double,          // 单根焦油量 mg
    val nicotineMg: Double,     // 单根尼古丁量 mg
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val pricePerCig: Double
        get() = if (cigsPerPack > 0) packPrice / cigsPerPack else 0.0
}

/**
 * 一次抽烟事件。关键字段做「快照」：即使以后改了烟价/焦油，
 * 历史统计依然按当时的数值计算，不会被回溯篡改。
 */
@Entity(tableName = "smoke_events")
data class SmokeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cigaretteId: Long,
    val cigaretteName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cost: Double,           // 这根烟花了多少钱（快照）
    val tarMg: Double,          // 摄入焦油 mg（快照）
    val nicotineMg: Double      // 摄入尼古丁 mg（快照）
)

/** 一次买烟记录。 */
@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cigaretteId: Long,
    val cigaretteName: String,
    val packs: Int,             // 买了几包
    val cigsCount: Int,         // 折合多少根（packs * cigsPerPack）
    val totalCost: Double,      // 总花费
    val timestamp: Long = System.currentTimeMillis()
)
