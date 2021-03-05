package name.lmj0011.jetpackreleasetracker.ui.libraries

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vdurmont.semver4j.Semver
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactDao
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdate
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibrary
import timber.log.Timber

class LibrariesViewModel(
    val database: AndroidXArtifactDao,
    application: Application
) : AndroidViewModel(application) {

    var artifacts = database.getAllAndroidXArtifactsObserverable()

    // A disgraceful hack to force Livedata to refresh itself, could probably
    // do this more gracefully using a Repository.
    fun refreshLibraries() {
        viewModelScope.launch(Dispatchers.IO) {
            var staleArtifact: AndroidXArtifact?

            // usually takes 1-2 seconds after LibraryRefreshWorker runs on a fresh install
            do {
                delay(1000L)
                staleArtifact = database.getAllAndroidXArtifacts().firstOrNull()
            } while (staleArtifact == null)

            Timber.d("staleArtifact: $staleArtifact")

            staleArtifact.let { it1 ->
                val freshArtifact = database.get(it1.id)
                freshArtifact?.let { it2 ->
                  database.update(it2)
                }
            }
        }
    }

    fun getAndroidXLibraryDataset(): List<AndroidXLibrary> {
        val artifacts = database.getAllAndroidXArtifacts()
        val libMap = mutableMapOf<String, AndroidXLibrary>()

        artifacts.forEach {
            val groupId = it.name.split(":")[0]
            val lib = libMap.getOrDefault(groupId,
                AndroidXLibrary(
                    groupIndexUrl = "",
                    releasePageUrl = it.releasePageUrl,
                    packageName = groupId,
                    artifactNames = mutableListOf()
                )
            )

            lib.artifactNames.add(it.packageName)
            libMap[groupId] = lib
        }

        return libMap.values.toList().reversed()
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

    // resets all library artifacts to the earliest version
    fun testAllNewerVersion() {
        artifacts.value?.map {
            it.latestStableVersion = "0.1.0-dev01"
            it.latestVersion = "0.1.0-dev01"
            it
        }?.toMutableList()
            ?.let {
                viewModelScope.launch(Dispatchers.IO) {
                    database.updateAll(it)
                }
            }
    }
}