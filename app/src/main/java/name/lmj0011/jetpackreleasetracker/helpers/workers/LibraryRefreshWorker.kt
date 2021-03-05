package name.lmj0011.jetpackreleasetracker.helpers.workers

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.helpers.GMavenXmlParser
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper
import name.lmj0011.jetpackreleasetracker.helpers.receivers.CancelWorkerByTagReceiver
import name.lmj0011.jetpackreleasetracker.ui.libraries.LibrariesViewModel
import timber.log.Timber
import kotlin.math.ceil

class LibraryRefreshWorker (private val appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {

    companion object {
        const val Progress = "Progress"
    }

    private var notificationCancelWorkerPendingIntent: PendingIntent

    init {
        val notificationCancelWorkerIntent = Intent(appContext, CancelWorkerByTagReceiver::class.java).apply {
            val tagArray = this@LibraryRefreshWorker.tags.toTypedArray()
            putExtra(appContext.getString(R.string.intent_extra_key_worker_tags), tagArray)
        }

        notificationCancelWorkerPendingIntent = PendingIntent.getBroadcast(appContext, 0, notificationCancelWorkerIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = appContext.applicationContext as Application
        val dataSource = AppDatabase.getInstance(appContext).androidXArtifactDao
        val librariesViewModel = LibrariesViewModel(dataSource, application)

        val foregroundNotification = NotificationCompat.Builder(applicationContext, NotificationHelper.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.notification_text_checking_for_newer_lib_versions))
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        val foregroundInfo = ForegroundInfo(NotificationHelper.UPDATES_NOTIFICATION_ID, foregroundNotification)
        setForeground(foregroundInfo)

        val artifactsToInsert = mutableListOf<AndroidXArtifact>()
        val artifactsToUpdate = mutableListOf<AndroidXArtifact>()
        val newArtifactVersionsToNotifySet = mutableSetOf<String>()

        val job = async {
            showProgress(0, appContext.getString(R.string.notification_text_fetching_artifacts))

            val localArtifacts = dataSource.getAllAndroidXArtifacts()
            val upstreamArtifactsList = GMavenXmlParser().loadArtifacts()

            if (upstreamArtifactsList.isNotEmpty()) {
                var progress = 0f
                // (progressIncrement * <collection>.size) should always equal a minimum of 100
                var progressIncrement =  (100f / upstreamArtifactsList.size)

                for (upstreamArtifact in upstreamArtifactsList) {
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

                    if (isStopped) {
                        Timber.d("isStopped: $isStopped")
                        break
                    } else {
                        showProgress(roundedUpProgress, upstreamArtifact.name)
                        setProgress(workDataOf(Progress to roundedUpProgress))
                    }


                    Timber.d("artifactName: ${upstreamArtifact.name}, progress: $roundedUpProgress")
                }
            }


            if(artifactsToInsert.isNotEmpty()) { dataSource.insertAll(artifactsToInsert) }
            if(artifactsToUpdate.isNotEmpty()) { dataSource.updateAll(artifactsToUpdate) }

            if (newArtifactVersionsToNotifySet.isNotEmpty()) {
                val summaryNotification = NotificationCompat.Builder(appContext, NotificationHelper.NEW_LIBRARY_VERSIONS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_new_releases_outline_24dp)
                    .setStyle(NotificationCompat.InboxStyle()
                        .setSummaryText(appContext.getString(R.string.notification_text_new_versions_available)))
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
                    .setGroup(NotificationHelper.NEW_LIBRARY_VERSIONS_NOTIFICATION_GROUP_ID)
                    .setGroupSummary(true)
                    .build()


                NotificationManagerCompat.from(appContext).apply {
                    notify(NotificationHelper.NEW_LIBRARY_VERSIONS_SUMMARY_NOTIFICATION_ID, summaryNotification)

                    for ((idx, str) in newArtifactVersionsToNotifySet.withIndex()) {
                        val artifact = localArtifacts?.find { art ->
                            val key = "${art.packageName} ${art.latestVersion}"
                            (str == key)
                        }

                        val notificationContentIntent = Intent(Intent.ACTION_VIEW, Uri.parse(artifact?.releasePageUrl))

                        val contentPendingIntent = PendingIntent.getActivity(appContext, 0, notificationContentIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                        val notification = NotificationCompat.Builder(appContext, NotificationHelper.NEW_LIBRARY_VERSIONS_CHANNEL_ID)
                            .setContentIntent(contentPendingIntent)
                            .setContentText(str)
                            .setSmallIcon(R.drawable.ic_new_releases_outline_24dp)
                            .setOnlyAlertOnce(true)
                            .setAutoCancel(true)
                            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
                            .setGroup(NotificationHelper.NEW_LIBRARY_VERSIONS_NOTIFICATION_GROUP_ID)
                            .build()

                        if (isStopped) {
                            Timber.d("isStopped: $isStopped")
                            break
                        } else {
                            notify(idx, notification)
                        }
                    }
                }
            }
        }

        job.await()

        if (isStopped) {
            NotificationManagerCompat.from(appContext).cancel(NotificationHelper.UPDATES_NOTIFICATION_ID)
        } else {
            // gives progress enough time to get passed to Observer(s)
            delay(1000L)
        }

        AppDatabase.closeInstance()
        Result.success()
    }

    private fun showProgress(progress: Int, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_sync_24)
            .setContentTitle(appContext.getString(R.string.notification_text_checking_for_newer_lib_versions))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .addAction(0, appContext.getString(R.string.notification_action_button_cancel), notificationCancelWorkerPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.UPDATES_NOTIFICATION_ID, notification)
        }
    }
}