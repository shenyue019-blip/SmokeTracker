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

/** 抽烟事件的类型。 */
object SmokeKind {
    const val SELF = "SELF"        // 自己抽自己的烟
    const val GIVE = "GIVE"        // 散烟：自己给别人（记花费，不记焦油/尼古丁，减库存）
    const val RECEIVE = "RECEIVE"  // 散烟：别人给自己（不记花费，记焦油/尼古丁与根数，不减库存）
}

/**
 * 一次抽烟/散烟事件。cost/tar/nicotine 为「快照」，仅在对应烟品被删除时作兜底；
 * 烟品仍存在时统计按其当前数值动态计算。
 */
@Entity(tableName = "smoke_events")
data class SmokeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cigaretteId: Long,
    val cigaretteName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cost: Double,           // 快照：这根烟花了多少钱
    val tarMg: Double,          // 快照：摄入焦油 mg
    val nicotineMg: Double,     // 快照：摄入尼古丁 mg
    val kind: String = SmokeKind.SELF
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
