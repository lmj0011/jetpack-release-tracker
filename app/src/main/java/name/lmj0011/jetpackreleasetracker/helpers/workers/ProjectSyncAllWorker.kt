package name.lmj0011.jetpackreleasetracker.helpers.workers

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Message
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper
import name.lmj0011.jetpackreleasetracker.ui.projectsyncs.ProjectSyncsViewModel
import timber.log.Timber
import kotlin.math.ceil

class ProjectSyncAllWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        const val Progress = "Progress"
    }

    override suspend fun doWork(): Result {
        val application = appContext.applicationContext as Application
        val dataSource = AppDatabase.getInstance(appContext).projectSyncDao
        val projectSyncViewModel = ProjectSyncsViewModel(dataSource, application)
        val projectSyncs = dataSource.getAllProjectSyncsForWorker()

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.PROJECT_SYNC_ALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle("Syncing all Projects")
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .build()

        val foregroundInfo = ForegroundInfo(NotificationHelper.PROJECT_SYNC_ALL_NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)

        if (projectSyncs.isNotEmpty()) {
            var progress = 0f
            // (progressIncrement * <collection>.size) should always equal a minimum of 100
            var progressIncrement =  (100f / projectSyncs.size)

            projectSyncs.forEach { ps ->
                projectSyncViewModel.synchronizeProject(ps).join()
                progress = progress.plus(progressIncrement)
                val roundedUpProgress = ceil(progress).toInt()
                showProgress(roundedUpProgress, ps.name)
                setProgress(workDataOf(Progress to roundedUpProgress))

                Timber.d("projectName: ${ps.name}, progress: $roundedUpProgress")
            }
        }

        // gives progress enough time to get passed to Observer(s)
        delay(1000L)
        AppDatabase.closeInstance()
        return Result.success()
    }

    private fun showProgress(progress: Int, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.PROJECT_SYNC_ALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle("Syncing all Projects")
            .setContentText(message)
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.PROJECT_SYNC_ALL_NOTIFICATION_ID, notification)
        }
    }

}