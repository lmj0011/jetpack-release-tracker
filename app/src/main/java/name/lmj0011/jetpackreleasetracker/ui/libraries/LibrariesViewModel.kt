package name.lmj0011.jetpackreleasetracker.ui.libraries

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.vdurmont.semver4j.Semver
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactDao
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdate
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibraryDataset
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXReleasePuller
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper
import timber.log.Timber
import java.lang.Exception

class LibrariesViewModel(
    val database: AndroidXArtifactDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    var artifacts = database.getAllAndroidXArtifacts()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // A disgraceful hack to force Livedata to refresh itself, could probably
    // do this more gracefully using a Repository.
    fun refreshLibraries() {
        uiScope.launch {
            var staleArtifact: AndroidXArtifact?

            // usually takes 1-2 seconds after LibraryRefreshWorker runs on a fresh install
            do {
                delay(1000L)
                staleArtifact = withContext(Dispatchers.IO) { database.getAllAndroidXArtifactsForWorker().firstOrNull() }
            } while (staleArtifact == null)

            Timber.d("staleArtifact: $staleArtifact")

            staleArtifact?.let {it1 ->
                val freshArtifact = withContext(Dispatchers.IO) { database.get(it1.id) }
                freshArtifact?.let { it2 ->
                    withContext(Dispatchers.IO) { database.update(it2) }
                }
            }
        }
    }

    private fun dropArtifactsTable() {
        database.clear()
    }

    fun normalRefresh(appContext: Context, notify: Boolean = false): Job {
       return uiScope.launch {
            withContext(Dispatchers.IO) {
                val list = fetchArtifacts()
                val artifactsToInsert = mutableListOf<AndroidXArtifact>()
                val artifactsToUpdate = mutableListOf<AndroidXArtifact>()
                val newArtifactVersionsToNotifySet = mutableSetOf<String>()

                when(list.size) {
                    0 -> {
                        hardRefresh()
                    }
                    else -> {
                        list.forEach {upstreamArtifact ->
                            val updatedArtifact = artifacts.value?.find { localArtifact ->
                                val upKey = "${upstreamArtifact.packageName}:${upstreamArtifact.name}"
                                val localKey = "${localArtifact.packageName}:${localArtifact.name}"
                                (upKey == localKey)
                            }.apply {
                                if (this != null){
                                    if(this@LibrariesViewModel.artifactHasNewerVersion(this, upstreamArtifact)){
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

                    }
                }

                if(artifactsToInsert.size > 0) { database.insertAll(artifactsToInsert) }
                if(artifactsToUpdate.size > 0) { database.updateAll(artifactsToUpdate) }

                if (notify && newArtifactVersionsToNotifySet.size > 0) {
                    withContext(Dispatchers.Main) {
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
                            .setAutoCancel(true)
                            .setColor(ContextCompat.getColor(appContext, R.color.colorPrimary))
                            .build()


                        NotificationManagerCompat.from(appContext).apply {
                            notify(NotificationHelper.UPDATES_NOTIFICATION_ID, notification)
                        }
                    }
                }
            }
        }
    }

    fun artifactHasNewerVersion(old: AndroidXArtifact, new: AndroidXArtifact): Boolean {
        return if (Semver(new.latestVersion, Semver.SemverType.LOOSE).isGreaterThan(old.latestVersion)) {
            val update = AndroidXArtifactUpdate().apply {
                name = new.name
                packageName = new.packageName
                releasePageUrl = new.releasePageUrl
                previousVersion = old.latestVersion
                latestVersion = new.latestVersion
            }

            database.insertArtifactUpdate(update)
            true
        } else {
            false
        }
    }

    private fun hardRefresh() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                val list = fetchArtifacts()
                dropArtifactsTable()
                database.insertAll(list)
            }
        }
    }

    fun fetchArtifacts(): MutableList<AndroidXArtifact> {
        val arp = AndroidXReleasePuller()
        val mList = mutableListOf<AndroidXArtifact>()

        AndroidXLibraryDataset.data.forEach {
            try {
                val map = arp.parseFeed(it)

                map?.let { thisMap ->
                    it.artifactNames.forEach { artifactName ->
                        val key = "${it.packageName}:$artifactName"

                        thisMap[key]?.let { m ->
                            val artifact = AndroidXArtifact().apply{
                                name = key
                                packageName = it.packageName
                                releasePageUrl = it.releasePageUrl
                                latestStableVersion = m["latestStableVersion"].toString()
                                latestVersion = m["latestVersion"].toString()
                            }
                            mList.add(artifact)
                        }
                    }
                }
            } catch(ex: Exception) {
                Timber.d("Failed to fetch artifacts from ${it.groupIndexUrl}")
                Timber.e(ex)
            }

        }

        return mList
    }

    // resets all library artifacts to the earliest version
    fun testAllNewerVersion() {
        artifacts.value?.map {
            it.latestStableVersion = "0.1.0-dev01"
            it.latestVersion = "0.1.0-dev01"
            it
        }?.toMutableList()
            ?.let {
                uiScope.launch {
                    withContext(Dispatchers.IO) {
                        database.updateAll(it)
                    }
                }
            }


    }
}