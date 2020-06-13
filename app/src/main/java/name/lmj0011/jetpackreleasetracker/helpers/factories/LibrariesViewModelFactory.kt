package name.lmj0011.jetpackreleasetracker.helpers.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactDao
import name.lmj0011.jetpackreleasetracker.ui.libraries.LibrariesViewModel

class LibrariesViewModelFactory(
    private val dataSource: AndroidXArtifactDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibrariesViewModel::class.java)) {
            return LibrariesViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}