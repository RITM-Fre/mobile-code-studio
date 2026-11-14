package com.example

import android.app.Application
import com.example.data.db.IdeDatabase
import com.example.data.fs.ProjectStorageManager
import com.example.data.prefs.SettingsRepository
import com.example.data.repository.IdeRepository
import com.example.server.LocalPreviewServerManager

class AppContainer(application: Application) {
    val database: IdeDatabase by lazy { IdeDatabase.getInstance(application) }
    val storageManager: ProjectStorageManager by lazy { ProjectStorageManager(application) }
    val ideRepository: IdeRepository by lazy { IdeRepository(database.ideDao(), storageManager) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
    val serverManager: LocalPreviewServerManager by lazy { LocalPreviewServerManager() }
}

class MobileCodeStudioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTerminate() {
        container.serverManager.stopServer()
        super.onTerminate()
    }
}
