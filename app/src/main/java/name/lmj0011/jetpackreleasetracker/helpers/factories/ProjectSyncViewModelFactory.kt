package name.lmj0011.jetpackreleasetracker.helpers.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.jetpackreleasetracker.database.ProjectSyncDao
import name.lmj0011.jetpackreleasetracker.ui.libraries.LibrariesViewModel
import name.lmj0011.jetpackreleasetracker.ui.projectsyncs.ProjectSyncsViewModel

class ProjectSyncViewModelFactory (
    private val dataSource: ProjectSyncDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectSyncsViewModel::class.java)) {
            return ProjectSyncsViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}