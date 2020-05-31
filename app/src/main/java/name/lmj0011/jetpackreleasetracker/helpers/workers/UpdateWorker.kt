package name.lmj0011.jetpackreleasetracker.helpers.workers

import android.app.Application
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.ui.libraries.LibrariesViewModel

class UpdateWorker (appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Do the work here
        val application = applicationContext as Application
        val dataSource = AppDatabase.getInstance(application).androidXArtifactDao
        val librariesViewModel = LibrariesViewModel(dataSource, application)

        librariesViewModel.normalRefresh(applicationContext, true)

        // Indicate whether the task finished successfully with the Result
        return Result.success()
    }
}