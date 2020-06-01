package name.lmj0011.jetpackreleasetracker.ui.libraries

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentLibrariesBinding
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibrary
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibraryDataset
import name.lmj0011.jetpackreleasetracker.helpers.adapters.AndroidXLibraryListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.HomeViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView
import timber.log.Timber

class LibrariesFragment : Fragment(),
    SearchableRecyclerView
{

    private lateinit var binding: FragmentLibrariesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var librariesViewModel: LibrariesViewModel
    private lateinit var listAdapter: AndroidXLibraryListAdapter
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  fragmentJob)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_libraries, container, false)
        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)


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

        binding.librariesSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                var list = AndroidXLibraryDataset.data.toMutableList()

                list?.let {
                    uiScope.launch {
                        val filteredList = withContext(Dispatchers.Default) {
                            listAdapter.filterBySearchQuery(newText, it)
                        }

                        this@LibrariesFragment.submitListToAdapter(filteredList)
                    }
                }
                return false
            }
        })

        binding.librariesSearchView.setOnCloseListener {
            this@LibrariesFragment.toggleSearch(mainActivity, binding.librariesSearchView, false)
            false
        }

        binding.librariesSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) { } else{
                binding.librariesSearchView.setQuery("", true)
                this@LibrariesFragment.toggleSearch(mainActivity, binding.librariesSearchView, false)
            }
        }


        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.testButton.visibility = View.GONE
        }

        librariesViewModel.normalRefresh(mainActivity, true)

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
    }

    private fun submitListToAdapter (list: MutableList<AndroidXLibrary>) {
        listAdapter.submitList(list)
        listAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.libraries_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_libraries_search -> {
                this@LibrariesFragment.toggleSearch(mainActivity, binding.librariesSearchView, true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
