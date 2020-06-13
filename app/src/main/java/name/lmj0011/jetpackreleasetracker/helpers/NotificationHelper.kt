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
    const val UPDATES_CHANNEL_ID = "name.lmj0011.bottomnavtinkering.helpers.NotificationHelper#updates"
    const val UPDATES_NOTIFICATION_ID = 1000

    const val DEBUG_CHANNEL_ID = "name.lmj0011.bottomnavtinkering.helpers.NotificationHelper#debug"
    const val DEBUG_NOTIFICATION_ID = 1001

    /**
     * create all necessary Notification channels here
     */
    fun init(application: Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val updatesServiceChannel = NotificationChannel(UPDATES_CHANNEL_ID, "Updates", NotificationManager.IMPORTANCE_DEFAULT)
            val debugServiceChannel = NotificationChannel(DEBUG_CHANNEL_ID, "Debug", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = application.getSystemService(NotificationManager::class.java)

            if(application.resources.getBoolean(R.bool.DEBUG_MODE)) {
                manager!!.createNotificationChannels(
                    mutableListOf(updatesServiceChannel, debugServiceChannel)
                )
            } else {
                manager!!.createNotificationChannels(
                    mutableListOf(updatesServiceChannel)
                )
            }

        }
    }

}