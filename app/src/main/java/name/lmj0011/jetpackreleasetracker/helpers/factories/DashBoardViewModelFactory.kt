package name.lmj0011.jetpackreleasetracker.helpers.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdateDao
import name.lmj0011.jetpackreleasetracker.ui.updates.UpdatesViewModel

class DashBoardViewModelFactory(
    private val dataSource: AndroidXArtifactUpdateDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpdatesViewModel::class.java)) {
            return UpdatesViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}