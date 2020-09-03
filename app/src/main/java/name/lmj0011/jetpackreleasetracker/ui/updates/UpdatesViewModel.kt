package name.lmj0011.jetpackreleasetracker.ui.updates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdateDao

class UpdatesViewModel(
    val database: AndroidXArtifactUpdateDao,
    application: Application
) : AndroidViewModel(application) {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }

    val text: LiveData<String> = _text

    var artifactUpdates = database.getAllAndroidXArtifactUpdates()

}