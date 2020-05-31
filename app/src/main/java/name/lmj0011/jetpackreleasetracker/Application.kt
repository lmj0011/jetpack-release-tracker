package name.lmj0011.jetpackreleasetracker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper
import name.lmj0011.jetpackreleasetracker.helpers.workers.UpdateWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class Application: android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        NotificationHelper.init(this)
        enqueueWorkers()
    }

    private fun enqueueWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)
        val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

//        workManager.cancelUniqueWork(applicationContext.getString(R.string.update_periodic_worker))
        workManager.enqueueUniquePeriodicWork(applicationContext.getString(R.string.update_periodic_worker), ExistingPeriodicWorkPolicy.KEEP, updateWorkRequest)
    }
}