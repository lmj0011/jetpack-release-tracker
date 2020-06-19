package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.app.Application
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.vdurmont.semver4j.Semver
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdateDao
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.database.ProjectSyncDao
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class ProjectSyncsViewModel(
    val database: ProjectSyncDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)
    val successMessages = MutableLiveData<String>()
    val errorMessages = MutableLiveData<String>()


    var projectSyncs = database.getAllProjectSyncs()

    val projectSync = MutableLiveData<ProjectSync?>()

    val projectDepsMap = MutableLiveData<MutableMap<String, MutableList<String>>>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // A disgraceful hack to force Livedata (ie. projectSyncs) to refresh itself, could probably
    // do this more gracefully using a Repository.
    fun refreshProjectSyncs() {
        uiScope.launch {
            val staleProjectSync = projectSyncs.value?.firstOrNull()

            staleProjectSync?.let {it1 ->
                val freshProjectSync = withContext(Dispatchers.IO) { database.get(it1.id) }
                freshProjectSync?.let { it2 ->
                    withContext(Dispatchers.IO) { database.update(it2) }
                }
            }
        }
    }

    fun insertProjectSync(
        pName: String,
        pDepListUrl: String
    ) {
        uiScope.launch {
            val project = ProjectSync().apply {
                name = pName
                depsListUrl = pDepListUrl
            }

            synchronizeProject(project).join()
        }

    }

    fun updateProjectSync(project: ProjectSync) {
        uiScope.launch {
            synchronizeProject(project).join()

            successMessages.postValue("${project.name} Saved")
        }

    }

    fun deleteProject(project: ProjectSync) {
        uiScope.launch {
            val rowId = withContext(Dispatchers.IO){
                this@ProjectSyncsViewModel.database.deleteByProjectSyncId(project.id)
            }
        }
    }

    fun setProjectSync(id: Long) {
        uiScope.launch {
            val project = withContext(Dispatchers.IO) {
                database.get(id)
            }

            projectSync.postValue(project)

            project?.let { synchronizeProject(it).join() }
        }
    }

    suspend fun synchronizeProject(project: ProjectSync): Job {
        // reset Project counters
        project.outdatedCount = 0
        project.upToDateCount = 0

        return uiScope.launch {
            // ref: https://github.com/kittinunf/fuel/blob/master/fuel/README.md#blocking-responses
            val res = withContext(Dispatchers.IO) { Fuel.get(project.depsListUrl).response() }
            val artifacts = withContext(Dispatchers.IO) { database.getAllAndroidXArtifacts() }

            val (data, error) = res.third

            error?.let { err -> errorMessages.postValue(err.message) }

            when {
                data is ByteArray -> {
                    val str = String(data)
                    val strLines = str.lines()
                    var resultProjectDepsMap: MutableMap<String, MutableList<String>> = mutableMapOf()

                    strLines.forEach {
                        val lib = it.split(':')
                        if (lib.size == 3) { // should only be 3 elements
                            val groupId = lib[0]
                            val artifactId = lib[1]
                            val versionId = lib[2]

                            if(resultProjectDepsMap["$groupId"] == null) resultProjectDepsMap["$groupId"] = mutableListOf()

                            artifacts.find { artifact ->
                                artifact.name == "$groupId:$artifactId"
                            }?.let { artifact ->

                                val str = if(project.stableVersionsOnly) {
                                    if (Semver(artifact.latestStableVersion, Semver.SemverType.LOOSE).isGreaterThan(versionId)) {
                                        project.outdatedCount = project.outdatedCount.plus(1)
                                        "$artifactId:$versionId -> ${artifact.latestStableVersion}\n"
                                    } else {
                                        project.upToDateCount = project.upToDateCount.plus(1)
                                        "$artifactId:$versionId\n"
                                    }
                                } else {
                                    if (Semver(artifact.latestVersion, Semver.SemverType.LOOSE).isGreaterThan(versionId)) {
                                        project.outdatedCount = project.outdatedCount.plus(1)
                                        "$artifactId:$versionId -> ${artifact.latestVersion}\n"
                                    }else {
                                        project.upToDateCount = project.upToDateCount.plus(1)
                                        "$artifactId:$versionId\n"
                                    }
                                }

                                resultProjectDepsMap["$groupId"]?.add(str)
                            }


                        }
                    }

                    // remove non-androidx entries
                    val keysToDelete = resultProjectDepsMap.keys.filter { str ->
                        !str.contains("androidx.")
                    }

                    keysToDelete.forEach { str ->
                        resultProjectDepsMap.remove(str)
                    }

                    projectDepsMap.postValue(resultProjectDepsMap)
                }
                error is FuelError -> {
                    error.exception?.let { Timber.e(it) }
                }
            }

            withContext(Dispatchers.IO) { this@ProjectSyncsViewModel.database.upsert(project) }
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

        uiScope.launch {
            withContext(Dispatchers.IO) {
                database.actualInsertAll(mutableListOf(proj1, proj2))
            }
        }
    }
}