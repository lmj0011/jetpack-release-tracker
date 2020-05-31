package name.lmj0011.jetpackreleasetracker.ui.libraries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentLibrariesBinding
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibraryDataset
import name.lmj0011.jetpackreleasetracker.helpers.adapters.AndroidXLibraryListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.HomeViewModelFactory
import timber.log.Timber

class LibrariesFragment : Fragment() {

    private lateinit var binding: FragmentLibrariesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var librariesViewModel: LibrariesViewModel
    private lateinit var listAdapter: AndroidXLibraryListAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_libraries, container, false)
        mainActivity = activity as MainActivity


        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application).androidXArtifactDao
        val viewModelFactory = HomeViewModelFactory(dataSource, application)
        librariesViewModel = ViewModelProvider(this, viewModelFactory).get(LibrariesViewModel::class.java)

        listAdapter = AndroidXLibraryListAdapter(
            AndroidXLibraryListAdapter.AndroidXLibraryListener {},
            AndroidXLibraryListAdapter.AndroidXLibraryStarListener {
                val spf = PreferenceManager.getDefaultSharedPreferences(mainActivity)
                // make a copy since original value can't reliably be modified; ref: https://stackoverflow.com/a/51001329/2445763
                val starredSet = (spf.getStringSet(this.getString(R.string.pref_key_starred_libraries), mutableSetOf<String>()) as MutableSet<String>).toMutableSet()

                if(starredSet.contains(it.packageName)){
                    starredSet.remove(it.packageName)
                } else {
                    starredSet.add(it.packageName)
                }

                spf.edit{
                    putStringSet(mainActivity.getString(R.string.pref_key_starred_libraries), starredSet)
                    apply()
                    listAdapter.notifyDataSetChanged()
                }
            }
        )

        binding.androidXLibraryList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))
        binding.androidXLibraryList.adapter = listAdapter
        binding.homeViewModel = librariesViewModel
        binding.lifecycleOwner = this


        listAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            private var cnt = 0

            // This gets called twice before list is fully populated with data
            override fun onChanged() {
                cnt++
                if (cnt > 1) {
                    mainActivity.mainCyclicProgressBar.visibility = View.GONE
                }
                Timber.d("RecyclerView.AdapterDataObserver.onChanged")
                super.onChanged()
            }
        })

        librariesViewModel.artifacts.observe(viewLifecycleOwner, Observer {
            listAdapter.submitLibArtifacts(it.toList())
            listAdapter.submitList(AndroidXLibraryDataset.data)
            listAdapter.notifyDataSetChanged()
        })

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.testButton.visibility = View.GONE
        }

        librariesViewModel.normalRefresh(mainActivity, true)

        return binding.root
    }
}
