package name.lmj0011.jetpackreleasetracker.helpers.workers

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
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
import name.lmj0011.jetpackreleasetracker.helpers.receivers.CancelWorkerByTagReceiver
import name.lmj0011.jetpackreleasetracker.ui.projectsyncs.ProjectSyncsViewModel
import timber.log.Timber
import kotlin.math.ceil

class ProjectSyncAllWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        const val Progress = "Progress"
    }

    private var notificationCancelWorkerPendingIntent: PendingIntent

    init {
        val notificationCancelWorkerIntent = Intent(appContext, CancelWorkerByTagReceiver::class.java).apply {
            val tagArray = this@ProjectSyncAllWorker.tags.toTypedArray()
            putExtra(appContext.getString(R.string.intent_extra_key_worker_tags), tagArray)
        }

        notificationCancelWorkerPendingIntent = PendingIntent.getBroadcast(appContext, 0, notificationCancelWorkerIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    override suspend fun doWork(): Result {
        val application = appContext.applicationContext as Application
        val dataSource = AppDatabase.getInstance(appContext).projectSyncDao
        val projectSyncViewModel = ProjectSyncsViewModel(dataSource, application)
        val projectSyncs = dataSource.getAllProjectSyncsForWorker()

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.PROJECT_SYNC_ALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.notification_text_syncing_all_projects))
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
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
            .setSmallIcon(R.drawable.ic_notif_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.notification_text_syncing_all_projects))
            .setContentText(message)
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.PROJECT_SYNC_ALL_NOTIFICATION_ID, notification)
        }
    }

}