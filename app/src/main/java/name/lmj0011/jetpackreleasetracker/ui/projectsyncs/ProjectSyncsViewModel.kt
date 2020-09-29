package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.vdurmont.semver4j.Semver
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.database.ProjectSyncDao
import timber.log.Timber
import kotlin.Exception
import kotlin.coroutines.CoroutineContext

class ProjectSyncsViewModel(
    val database: ProjectSyncDao,
    application: Application
) : AndroidViewModel(application) {

    val successMessages = MutableLiveData<String>()
    val errorMessages = MutableLiveData<String>()
    var projectSyncs = database.getAllProjectSyncs()
    val projectSync = MutableLiveData<ProjectSync?>()
    val projectDepsMap = MutableLiveData<MutableMap<String, MutableList<String>>>()

    // A disgraceful hack to force Livedata (ie. projectSyncs) to refresh itself, could probably
    // do this more gracefully using a Repository.
    fun refreshProjectSyncs() {
        viewModelScope.launch(Dispatchers.IO) {
            var staleProjectSync: ProjectSync?

            do {
                delay(1000L)
                staleProjectSync = database.getAllProjectSyncsForWorker().firstOrNull()
            } while (staleProjectSync == null)

            Timber.d("staleProjectSync: $staleProjectSync")

            staleProjectSync.let { it1 ->
                val freshProjectSync = database.get(it1.id)
                freshProjectSync?.let { it2 ->
                    database.update(it2)
                }
            }
        }
    }

    fun insertProjectSync(
        pName: String,
        pDepListUrl: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectSync().apply {
                name = pName
                depsListUrl = pDepListUrl
            }
            synchronizeProject(project).join()
        }

    }

    fun updateProjectSync(project: ProjectSync) {
        viewModelScope.launch {
            synchronizeProject(project).join()
            successMessages.postValue("${project.name} Saved")
        }
    }

    fun deleteProject(project: ProjectSync) {
        viewModelScope.launch(Dispatchers.IO) {
            database.deleteByProjectSyncId(project.id)
        }
    }

    fun setProjectSync(id: Long) {
        viewModelScope.launch {
            val project = database.get(id)
            projectSync.postValue(project)
            project?.let {
                synchronizeProject(project).join()
            }
        }
    }

    fun synchronizeProject(project: ProjectSync): Job {
        // reset Project counters
        project.outdatedCount = 0
        project.upToDateCount = 0

        return viewModelScope.launch(Dispatchers.IO) {
            lateinit var res: ResponseResultOf<ByteArray>

            try {
                // ref: https://github.com/kittinunf/fuel/blob/master/fuel/README.md#blocking-responses
                res = Fuel.get(project.depsListUrl).response()
            } catch (ex: Throwable) {
                errorMessages.postValue(ex.message)
                return@launch
            }

            val artifacts = database.getAllAndroidXArtifacts()

            val (data, error) = res.third

            error?.let { err -> errorMessages.postValue(err.message) }

            when {
                data is ByteArray -> {
                    val str = String(data)
                    val strLines = str.lines()
                    val resultProjectDepsMap: MutableMap<String, MutableList<String>> =
                        mutableMapOf()

                    strLines.forEach {
                        val lib = it.split(':')
                        if (lib.size == 3) { // should only be 3 elements
                            val groupId = lib[0]
                            val artifactId = lib[1]
                            val versionId = lib[2]

                            if (resultProjectDepsMap[groupId] == null) resultProjectDepsMap[groupId] =
                                mutableListOf()

                            artifacts.find { artifact ->
                                artifact.name == "$groupId:$artifactId"
                            }?.let { artifact ->

                                val str = if (project.stableVersionsOnly) {
                                    if (Semver(
                                            artifact.latestStableVersion,
                                            Semver.SemverType.LOOSE
                                        ).isGreaterThan(versionId)
                                    ) {
                                        project.outdatedCount = project.outdatedCount.plus(1)
                                        "$artifactId:$versionId -> ${artifact.latestStableVersion}\n"
                                    } else {
                                        project.upToDateCount = project.upToDateCount.plus(1)
                                        "$artifactId:$versionId\n"
                                    }
                                } else {
                                    if (Semver(
                                            artifact.latestVersion,
                                            Semver.SemverType.LOOSE
                                        ).isGreaterThan(versionId)
                                    ) {
                                        project.outdatedCount = project.outdatedCount.plus(1)
                                        "$artifactId:$versionId -> ${artifact.latestVersion}\n"
                                    } else {
                                        project.upToDateCount = project.upToDateCount.plus(1)
                                        "$artifactId:$versionId\n"
                                    }
                                }

                                resultProjectDepsMap[groupId]?.add(str)
                            }


                        }
                    }

                    // remove non-androidx entries
                    val keysToDelete = resultProjectDepsMap.keys.filter { ktd ->
                        !ktd.contains("androidx.")
                    }

                    keysToDelete.forEach { element ->
                        resultProjectDepsMap.remove(element)
                    }

                    projectDepsMap.postValue(resultProjectDepsMap)
                }
                error is FuelError -> {
                    error.exception.let { Timber.e(it) }
                }
            }

            database.upsert(project)
        }
    }

    fun test() {
        val proj1 = ProjectSync().apply {
            name = "AndroidApp1"
            upToDateCount = 5
            outdatedCount = 2

        }

        val proj2 = ProjectSync().apply {
            name = "AndroidApp2"
            upToDateCount = 8
            outdatedCount = 0
        }

        viewModelScope.launch(Dispatchers.IO) {
            database.actualInsertAll(mutableListOf(proj1, proj2))
        }
    }
}