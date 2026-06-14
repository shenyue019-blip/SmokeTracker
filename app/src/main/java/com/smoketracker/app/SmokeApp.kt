package com.smoketracker.app

import android.app.Application
import com.smoketracker.app.data.AppDatabase
import com.smoketracker.app.data.SmokeRepository
import com.smoketracker.app.widget.SmokeWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class SmokeApp : Application() {
    lateinit var repository: SmokeRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = SmokeRepository(db.cigaretteDao(), db.smokeEventDao(), db.purchaseDao())

        // App 内抽烟/撤销/编辑后，自动刷新桌面小组件的今日根数
        appScope.launch {
            repository.events.drop(1).collect {
                SmokeWidgetProvider.refresh(this@SmokeApp)
            }
        }
    }
}
