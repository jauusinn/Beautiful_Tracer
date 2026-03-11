package com.beautifultracer.app

import android.app.Application
import com.beautifultracer.app.data.local.AppDatabase

class App : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
