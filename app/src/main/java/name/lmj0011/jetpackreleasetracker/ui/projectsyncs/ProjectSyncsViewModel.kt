package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdateDao
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.database.ProjectSyncDao

class ProjectSyncsViewModel(
    val database: ProjectSyncDao,
    application: Application
) : AndroidViewModel(application) {
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    var projectSyncs = database.getAllProjectSyncs()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
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