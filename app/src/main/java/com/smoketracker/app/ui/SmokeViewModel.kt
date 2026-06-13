package com.smoketracker.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smoketracker.app.data.Cigarette
import com.smoketracker.app.data.Purchase
import com.smoketracker.app.data.SmokeEvent
import com.smoketracker.app.data.SmokeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmokeViewModel(private val repo: SmokeRepository) : ViewModel() {

    val cigarettes: StateFlow<List<Cigarette>> =
        repo.cigarettes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultCigarette: StateFlow<Cigarette?> =
        repo.defaultCigarette.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val events: StateFlow<List<SmokeEvent>> =
        repo.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> =
        repo.purchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun smokeOne(cig: Cigarette) = viewModelScope.launch { repo.smokeOne(cig) }

    fun undoLast(onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        onResult(repo.undoLastSmoke())
    }

    fun addCigarette(
        name: String,
        packPrice: Double,
        cigsPerPack: Int,
        tarMg: Double,
        nicotineMg: Double
    ) = viewModelScope.launch {
        repo.addOrUpdateCigarette(
            Cigarette(
                name = name.trim(),
                packPrice = packPrice,
                cigsPerPack = cigsPerPack,
                tarMg = tarMg,
                nicotineMg = nicotineMg
            )
        )
    }

    /** 编辑已有烟品（后补价格/焦油/尼古丁等）。 */
    fun updateCigarette(
        original: Cigarette,
        name: String,
        packPrice: Double,
        cigsPerPack: Int,
        tarMg: Double,
        nicotineMg: Double
    ) = viewModelScope.launch {
        repo.updateCigarette(
            original.copy(
                name = name.trim(),
                packPrice = packPrice,
                cigsPerPack = cigsPerPack,
                tarMg = tarMg,
                nicotineMg = nicotineMg
            )
        )
    }

    fun setDefault(cig: Cigarette) = viewModelScope.launch { repo.setDefaultCigarette(cig.id) }

    fun recordPurchase(cig: Cigarette, packs: Int, totalCost: Double) =
        viewModelScope.launch { repo.recordPurchase(cig, packs, totalCost) }

    fun deleteEvent(e: SmokeEvent) = viewModelScope.launch { repo.deleteEvent(e) }
    fun deletePurchase(p: Purchase) = viewModelScope.launch { repo.deletePurchase(p) }

    class Factory(private val repo: SmokeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SmokeViewModel(repo) as T
        }
    }
}
