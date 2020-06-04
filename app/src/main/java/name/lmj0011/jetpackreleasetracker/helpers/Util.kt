package name.lmj0011.jetpackreleasetracker.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import name.lmj0011.jetpackreleasetracker.MainActivity


class Util {
    companion object {
        fun openUrlInWebBrowser(activity: MainActivity, url: String) {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            }
        }
    }
}
