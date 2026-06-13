package com.smoketracker.app.data

import kotlinx.coroutines.flow.Flow

/**
 * 单一数据入口。UI / ViewModel 只跟它打交道。
 */
class SmokeRepository(
    private val cigaretteDao: CigaretteDao,
    private val eventDao: SmokeEventDao,
    private val purchaseDao: PurchaseDao
) {
    val cigarettes: Flow<List<Cigarette>> = cigaretteDao.observeAll()
    val defaultCigarette: Flow<Cigarette?> = cigaretteDao.observeDefault()
    val events: Flow<List<SmokeEvent>> = eventDao.observeAll()
    val purchases: Flow<List<Purchase>> = purchaseDao.observeAll()

    suspend fun addOrUpdateCigarette(c: Cigarette): Long {
        val isFirst = cigaretteDao.count() == 0
        val id = cigaretteDao.insert(c.copy(isDefault = c.isDefault || isFirst))
        // 第一支烟自动设为默认，省去用户一步操作。
        if (isFirst) cigaretteDao.setDefault(id)
        return id
    }

    suspend fun updateCigarette(c: Cigarette) = cigaretteDao.update(c)

    suspend fun setDefaultCigarette(id: Long) = cigaretteDao.setDefault(id)

    /** 按类型构造一条事件的快照数值。 */
    private fun eventOf(cig: Cigarette, kind: String): SmokeEvent = when (kind) {
        SmokeKind.GIVE -> SmokeEvent(    // 散给别人：记花费，不记焦油/尼古丁
            cigaretteId = cig.id, cigaretteName = cig.name,
            cost = cig.pricePerCig, tarMg = 0.0, nicotineMg = 0.0, kind = kind
        )
        SmokeKind.RECEIVE -> SmokeEvent( // 别人给的：不记花费，记焦油/尼古丁
            cigaretteId = cig.id, cigaretteName = cig.name,
            cost = 0.0, tarMg = cig.tarMg, nicotineMg = cig.nicotineMg, kind = kind
        )
        else -> SmokeEvent(              // 自抽：全记
            cigaretteId = cig.id, cigaretteName = cig.name,
            cost = cig.pricePerCig, tarMg = cig.tarMg, nicotineMg = cig.nicotineMg, kind = SmokeKind.SELF
        )
    }

    /** 抽一根（自抽）。 */
    suspend fun smokeOne(cig: Cigarette): Long = eventDao.insert(eventOf(cig, SmokeKind.SELF))

    /** 散给别人一根。 */
    suspend fun giveAway(cig: Cigarette): Long = eventDao.insert(eventOf(cig, SmokeKind.GIVE))

    /** 别人给自己一根（抽了）。 */
    suspend fun receiveOne(cig: Cigarette): Long = eventDao.insert(eventOf(cig, SmokeKind.RECEIVE))

    /** 撤销上一根自抽（误触救星，只撤自抽，不影响散烟记录）。 */
    suspend fun undoLastSmoke(): Boolean {
        val latest = eventDao.getLatestOfKind(SmokeKind.SELF) ?: return false
        eventDao.delete(latest)
        return true
    }

    suspend fun recordPurchase(cig: Cigarette, packs: Int, totalCost: Double): Long {
        return purchaseDao.insert(
            Purchase(
                cigaretteId = cig.id,
                cigaretteName = cig.name,
                packs = packs,
                cigsCount = packs * cig.cigsPerPack,
                totalCost = totalCost
            )
        )
    }

    suspend fun deletePurchase(p: Purchase) = purchaseDao.delete(p)
    suspend fun deleteEvent(e: SmokeEvent) = eventDao.delete(e)

    /** 编辑一条事件：改烟品/时间，按其原本类型重算快照。 */
    suspend fun updateEvent(original: SmokeEvent, cig: Cigarette, timestamp: Long) {
        val rebuilt = eventOf(cig, original.kind).copy(id = original.id, timestamp = timestamp)
        eventDao.update(rebuilt)
    }

    suspend fun updatePurchase(p: Purchase) = purchaseDao.update(p)
}
