package name.lmj0011.jetpackreleasetracker.helpers.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import name.lmj0011.jetpackreleasetracker.R
import timber.log.Timber

class CancelWorkerByTagReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tagsArray = intent.getStringArrayExtra(context.getString(R.string.intent_extra_key_worker_tags))!!

        Timber.d("tagsArray [${tagsArray.joinToString()}]")

        when {
            tagsArray.contains(context.getString(R.string.update_periodic_worker_tag)) -> {
                WorkManager.getInstance(context).cancelAllWorkByTag(context.getString(R.string.update_periodic_worker_tag))
            }
            tagsArray.contains(context.getString(R.string.update_one_time_worker_tag)) -> {
                WorkManager.getInstance(context).cancelAllWorkByTag(context.getString(R.string.update_one_time_worker_tag))
            }
            tagsArray.contains(context.getString(R.string.project_sync_all_one_time_worker_tag)) -> {
                WorkManager.getInstance(context).cancelAllWorkByTag(context.getString(R.string.project_sync_all_one_time_worker_tag))
            }
        }

    }
}