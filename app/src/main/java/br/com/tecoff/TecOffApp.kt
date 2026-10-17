package br.com.tecoff

import android.app.Application
import br.com.tecoff.data.local.AppDatabase
import br.com.tecoff.data.local.ImageStorageManager
import br.com.tecoff.data.remote.SessionManager
import br.com.tecoff.data.remote.TecApiService
import br.com.tecoff.data.remote.TecHttpClient
import br.com.tecoff.domain.SyncManager

class TecOffApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var tecHttpClient: TecHttpClient
        private set
    lateinit var apiService: TecApiService
        private set
    lateinit var imageStorageManager: ImageStorageManager
        private set
    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        sessionManager = SessionManager(this)
        tecHttpClient = TecHttpClient(sessionManager)
        apiService = TecApiService(tecHttpClient, sessionManager)
        imageStorageManager = ImageStorageManager(this, tecHttpClient.okHttpClient)
        syncManager = SyncManager(database, apiService, imageStorageManager)
    }
}
