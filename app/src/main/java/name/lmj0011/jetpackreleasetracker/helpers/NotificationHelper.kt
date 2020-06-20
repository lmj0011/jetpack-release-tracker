package name.lmj0011.jetpackreleasetracker.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import name.lmj0011.jetpackreleasetracker.Application
import name.lmj0011.jetpackreleasetracker.R

/**
 *  This is a static class mainly for calling functions that return a NotificationCompat.Builder. If you're
 *  calling the same notification in more than one place, then save some code by calling its Builder from here
 */
object NotificationHelper {
    const val UPDATES_CHANNEL_ID = "name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper#updates-v1"
    const val UPDATES_NOTIFICATION_ID = 1000

    const val DEBUG_CHANNEL_ID = "name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper#debug"
    const val DEBUG_NOTIFICATION_ID = 1001

    const val PROJECT_SYNC_ALL_CHANNEL_ID = "name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper#project-sync-all"
    const val PROJECT_SYNC_ALL_NOTIFICATION_ID = 1002

    const val NEW_LIBRARY_VERSIONS_CHANNEL_ID = "name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper#new-library-versions"
    const val NEW_LIBRARY_VERSIONS_NOTIFICATION_ID = 1003

    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val updatesServiceChannel = NotificationChannel(UPDATES_CHANNEL_ID, "Updates", NotificationManager.IMPORTANCE_DEFAULT)
            updatesServiceChannel.setSound(null, null)

            val debugServiceChannel = NotificationChannel(DEBUG_CHANNEL_ID, "Debug", NotificationManager.IMPORTANCE_DEFAULT)
            debugServiceChannel.setSound(null,null)

            val projectSyncAllServiceChannel = NotificationChannel(PROJECT_SYNC_ALL_CHANNEL_ID, "Project Sync All", NotificationManager.IMPORTANCE_DEFAULT)
            projectSyncAllServiceChannel.setSound(null,null)

            val newLibraryVersionsNotificationChannel = NotificationChannel(NEW_LIBRARY_VERSIONS_CHANNEL_ID, "New Library Versions", NotificationManager.IMPORTANCE_DEFAULT)

            val manager = application.getSystemService(NotificationManager::class.java)

            purgeOldChannels(manager)

            if(application.resources.getBoolean(R.bool.DEBUG_MODE)) {
                manager!!.createNotificationChannels(
                    mutableListOf(updatesServiceChannel, debugServiceChannel, projectSyncAllServiceChannel,newLibraryVersionsNotificationChannel)
                )
            } else {
                manager!!.createNotificationChannels(
                    mutableListOf(updatesServiceChannel, projectSyncAllServiceChannel, newLibraryVersionsNotificationChannel)
                )
            }

        }
    }

    private fun purgeOldChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel("name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper#updates")
            manager.deleteNotificationChannel("name.lmj0011.bottomnavtinkering.helpers.NotificationHelper#updates")
            manager.deleteNotificationChannel("name.lmj0011.bottomnavtinkering.helpers.NotificationHelper#debug")
        }
    }

}