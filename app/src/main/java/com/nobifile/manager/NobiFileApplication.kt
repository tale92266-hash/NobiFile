package com.nobifile.manager

import android.app.Application
import com.nobifile.manager.data.AppDatabase
import com.nobifile.manager.data.PrefsStore
import com.nobifile.manager.ops.OperationManager

class NobiFileApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var prefs: PrefsStore
    lateinit var operations: OperationManager

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase(this)
        prefs = PrefsStore(this)
        operations = OperationManager(this, database)
    }
}
