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

    /** 抽一根：按当前烟品数值生成快照事件。 */
    suspend fun smokeOne(cig: Cigarette): Long {
        return eventDao.insert(
            SmokeEvent(
                cigaretteId = cig.id,
                cigaretteName = cig.name,
                cost = cig.pricePerCig,
                tarMg = cig.tarMg,
                nicotineMg = cig.nicotineMg
            )
        )
    }

    /** 撤销上一根（误触救星）。 */
    suspend fun undoLastSmoke(): Boolean {
        val latest = eventDao.getLatest() ?: return false
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

    suspend fun updateEvent(e: SmokeEvent) = eventDao.update(e)
    suspend fun updatePurchase(p: Purchase) = purchaseDao.update(p)
}
