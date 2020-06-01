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
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactDao
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdate
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibraryDataset
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXReleasePuller
import name.lmj0011.jetpackreleasetracker.helpers.NotificationHelper

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

    private fun dropArtifactsTable() {
        database.clear()
    }

    fun normalRefresh(appContext: Context? = null, notify: Boolean = false) {
       uiScope.launch {
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
                                        newArtifactVersionsToNotifySet.add("${this.packageName} ${upstreamArtifact.latestVersion}")
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

                if (notify && appContext is Context && newArtifactVersionsToNotifySet.size > 0) {
                    val notificationContentIntent = Intent(appContext, MainActivity::class.java).apply {
                        putExtra("menuItemId", R.id.navigation_updates)
                    }
                    val contentPendingIntent = PendingIntent.getActivity(appContext, 0, notificationContentIntent, PendingIntent.FLAG_CANCEL_CURRENT)


                    val notification = NotificationCompat.Builder(appContext, NotificationHelper.UPDATES_CHANNEL_ID)
                        .setContentTitle("New Versions Available!")
                        .setContentIntent(contentPendingIntent)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(
                            newArtifactVersionsToNotifySet.joinToString("") { n -> "$n\n" })
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
        }
    }

    private fun artifactHasNewerVersion(old: AndroidXArtifact, new: AndroidXArtifact): Boolean {
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

    private fun fetchArtifacts(): MutableList<AndroidXArtifact> {
        val arp = AndroidXReleasePuller()
        val mList = mutableListOf<AndroidXArtifact>()

        AndroidXLibraryDataset.data.forEach {
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
        }

        return mList
    }

    // TODO remove
    // DB needs to be populated with the following artifacts for this to work
    fun testNewerVersion() {
        val artifact1 = artifacts.value?.find {
            ((it.packageName == "androidx.activity") && (it.name == "androidx.activity:activity"))
        }.apply {
            this?.latestVersion = "1.0.0"
        }

        val artifact2 = artifacts.value?.find {
            ((it.packageName == "androidx.activity") && (it.name == "androidx.activity:activity-ktx"))
        }.apply {
            this?.latestVersion = "1.0.0"
        }

        val artifact3 = artifacts.value?.find {
            ((it.packageName == "androidx.compose") && (it.name == "androidx.compose:compose-compiler"))
        }.apply {
            this?.latestVersion = "0.1.0-dev04"
        }

        val artifact4 = artifacts.value?.find {
            ((it.packageName == "androidx.core") && (it.name == "androidx.core:core"))
        }.apply {
            this?.latestVersion = "1.0.0"
        }


        uiScope.launch {
            val list = mutableListOf(artifact1!!, artifact2!!, artifact3!!, artifact4!!)
            withContext(Dispatchers.IO) {
                database.updateAll(list)
            }
        }
    }
}