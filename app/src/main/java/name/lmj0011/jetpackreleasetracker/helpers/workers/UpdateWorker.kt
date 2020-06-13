package name.lmj0011.jetpackreleasetracker.helpers.workers

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper
import name.lmj0011.jetpackreleasetracker.ui.libraries.LibrariesViewModel
import timber.log.Timber

class UpdateWorker (private val appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    var debugMessage = "${this::class.simpleName} performed work."

    override suspend fun doWork(): Result = coroutineScope {
        // Do the work here
        val application = appContext.applicationContext as Application
        val dataSource = AppDatabase.getInstance(appContext).androidXArtifactDao

        /**
         * We're only using this viewModel for some of it's methods that don't involve live data.
         *  Live data doesn't work as expected inside a Worker, since there it's not a lifecycleOwner
         */
        val librariesViewModel = LibrariesViewModel(dataSource, application)

        val job = async {
            val localArtifacts = dataSource.getAllAndroidXArtifactsForWorker()
            val upstreamArtifactsList = librariesViewModel.fetchArtifacts()
            val artifactsToInsert = mutableListOf<AndroidXArtifact>()
            val artifactsToUpdate = mutableListOf<AndroidXArtifact>()
            val newArtifactVersionsToNotifySet = mutableSetOf<String>()

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
            }

            if(artifactsToInsert.size > 0) { dataSource.insertAll(artifactsToInsert) }
            if(artifactsToUpdate.size > 0) { dataSource.updateAll(artifactsToUpdate) }

            if (newArtifactVersionsToNotifySet.size > 0) {
                val notificationContentIntent = Intent(appContext, MainActivity::class.java).apply {
                    putExtra("menuItemId", R.id.navigation_updates)
                }
                val contentPendingIntent = PendingIntent.getActivity(appContext, 0, notificationContentIntent, PendingIntent.FLAG_CANCEL_CURRENT)


                val notification = NotificationCompat.Builder(appContext, NotificationHelper.UPDATES_CHANNEL_ID)
                    .setContentTitle("New Versions Available!")
                    .setContentIntent(contentPendingIntent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(
                        newArtifactVersionsToNotifySet.joinToString("") { n -> "\n$n\n" })
                    )
                    .setSmallIcon(R.drawable.ic_new_releases_outline_24dp)
                    .setOnlyAlertOnce(true)
                    .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
                    .build()


                NotificationManagerCompat.from(appContext).apply {
                    notify(NotificationHelper.UPDATES_NOTIFICATION_ID, notification)
                }
            }
        }

        job.await()
        debugStatusNotification(debugMessage)
        Result.success()
    }

    private fun debugStatusNotification(message: String) {
        if(!appContext.resources.getBoolean(R.bool.DEBUG_MODE)) return

        val notification = NotificationCompat.Builder(appContext, NotificationHelper.DEBUG_CHANNEL_ID)
            .setContentTitle(appContext.resources.getString(R.string.debug_notification_content_title))
            .setContentText(message)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_baseline_bug_report_24)
            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
            .build()

        NotificationManagerCompat.from(appContext).apply {
            notify(NotificationHelper.DEBUG_NOTIFICATION_ID, notification)
        }
    }
}