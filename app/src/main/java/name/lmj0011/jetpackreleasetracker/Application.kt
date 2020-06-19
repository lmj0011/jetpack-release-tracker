package name.lmj0011.jetpackreleasetracker

import androidx.work.*
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

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // runs once, anytime during the last 15 minutes of every hour
        )
            .setInitialDelay(2, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(applicationContext.getString(R.string.update_periodic_worker), ExistingPeriodicWorkPolicy.REPLACE, updateWorkRequest)
    }
}