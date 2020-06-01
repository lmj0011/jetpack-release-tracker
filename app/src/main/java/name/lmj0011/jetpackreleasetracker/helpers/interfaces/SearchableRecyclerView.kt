package name.lmj0011.jetpackreleasetracker.helpers.interfaces

import android.view.View
import androidx.appcompat.widget.SearchView
import name.lmj0011.jetpackreleasetracker.MainActivity

interface SearchableRecyclerView {

    fun toggleSearch(mainActivity: MainActivity, searchView: SearchView?, giveFocus: Boolean) {
        searchView?.let {
            if(giveFocus) {
                it.visibility = View.VISIBLE
                mainActivity.supportActionBar?.hide()

                // dumb hack https://stackoverflow.com/a/47287337/2445763
                it.isIconified = false
                mainActivity.showKeyBoard(it)
            } else {
                it.clearFocus()
                it.visibility = View.GONE
                mainActivity.supportActionBar?.show()
                mainActivity.hideKeyBoard(it)
            }
        }
    }
}