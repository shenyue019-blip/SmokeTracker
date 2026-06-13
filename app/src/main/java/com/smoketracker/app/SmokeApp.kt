package com.smoketracker.app

import android.app.Application
import com.smoketracker.app.data.AppDatabase
import com.smoketracker.app.data.SmokeRepository

class SmokeApp : Application() {
    lateinit var repository: SmokeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = SmokeRepository(db.cigaretteDao(), db.smokeEventDao(), db.purchaseDao())
    }
}
