package name.lmj0011.jetpackreleasetracker.helpers.workers

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper
import name.lmj0011.jetpackreleasetracker.ui.libraries.LibrariesViewModel
import timber.log.Timber
import kotlin.math.ceil

class LibraryRefreshWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        const val Progress = "Progress"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = appContext.applicationContext as Application
        val dataSource = AppDatabase.getInstance(appContext).androidXArtifactDao
        val librariesViewModel = LibrariesViewModel(dataSource, application)

        val foregroundNotification = NotificationCompat.Builder(applicationContext, NotificationHelper.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle("Checking for new library versions")
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .build()

        val foregroundInfo = ForegroundInfo(NotificationHelper.UPDATES_NOTIFICATION_ID, foregroundNotification)
        setForeground(foregroundInfo)

        val artifactsToInsert = mutableListOf<AndroidXArtifact>()
        val artifactsToUpdate = mutableListOf<AndroidXArtifact>()
        val newArtifactVersionsToNotifySet = mutableSetOf<String>()

        val job = async {
            val localArtifacts = dataSource.getAllAndroidXArtifactsForWorker()
            val upstreamArtifactsList = librariesViewModel.fetchArtifacts()

            if (upstreamArtifactsList.isNotEmpty()) {
                var progress = 0f
                // (progressIncrement * <collection>.size) should always equal a minimum of 100
                var progressIncrement =  (100f / upstreamArtifactsList.size)

                upstreamArtifactsList.forEach {upstreamArtifact ->
                    val updatedArtifact = localArtifacts?.find { localArtifact ->
                        val upKey = "${upstreamArtifact.packageName}:${upstreamArtifact.name}"
                        val localKey = "${localArtifact.packageName}:${localArtifact.name}"
                        (upKey == localKey)
                    }.apply {
                        if (this != null){
                            if(librariesViewModel.artifactHasNewerVersion(this, upstreamArtifact)){
                                val notifyStr = "${this.packageName} ${upstreamArtifact.latestVersion}"
                                newArtifactVersionsToNotifySet.add(notifyStr)
                                Timber.d("$notifyStr was added to newArtifactVersionsToNotifySet!")
                            }

                            this.latestStableVersion = upstreamArtifact.latestStableVersion
                            this.latestVersion = upstreamArtifact.latestVersion
                        }
                    }

                    if (updatedArtifact != null) {
                        artifactsToUpdate.add(updatedArtifact)
                    } else {
                        artifactsToInsert.add(upstreamArtifact)
                    }

                    progress = progress.plus(progressIncrement)
                    val roundedUpProgress = ceil(progress).toInt()
                    showProgress(roundedUpProgress, upstreamArtifact.name)
                    setProgress(workDataOf(ProjectSyncAllWorker.Progress to roundedUpProgress))

                    Timber.d("artifactName: ${upstreamArtifact.name}, progress: $roundedUpProgress")
                }
            }


            if(artifactsToInsert.isNotEmpty()) { dataSource.insertAll(artifactsToInsert) }
            if(artifactsToUpdate.isNotEmpty()) { dataSource.updateAll(artifactsToUpdate) }

            if (newArtifactVersionsToNotifySet.isNotEmpty()) {
                val notificationContentIntent = Intent(appContext, MainActivity::class.java).apply {
                    putExtra("menuItemId", R.id.navigation_updates)
                }
                val contentPendingIntent = PendingIntent.getActivity(appContext, 0, notificationContentIntent, PendingIntent.FLAG_CANCEL_CURRENT)


                val notification = NotificationCompat.Builder(appContext, NotificationHelper.NEW_LIBRARY_VERSIONS_CHANNEL_ID)
                    .setContentTitle("New Versions Available!")
                    .setContentIntent(contentPendingIntent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(
                        newArtifactVersionsToNotifySet.joinToString("") { n -> "\n$n\n" })
                    )
                    .setSmallIcon(R.drawable.ic_new_releases_outline_24dp)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
                    .build()


                NotificationManagerCompat.from(appContext).apply {
                    notify(NotificationHelper.NEW_LIBRARY_VERSIONS_NOTIFICATION_ID, notification)
                }
            }
        }

        job.await()

        // gives progress enough time to get passed to Observer(s)
        delay(1000L)
        AppDatabase.closeInstance()
        Result.success()
    }

    private fun showProgress(progress: Int, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle("Checking for new library versions")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.UPDATES_NOTIFICATION_ID, notification)
        }
    }
}