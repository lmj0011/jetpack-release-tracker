package name.lmj0011.jetpackreleasetracker.ui.updates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdate
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentUpdatesBinding
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibrary
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibraryDataset
import name.lmj0011.jetpackreleasetracker.helpers.Util
import name.lmj0011.jetpackreleasetracker.helpers.adapters.AndroidXArtifactUpdateListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.DashBoardViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView

class UpdatesFragment : Fragment(),
    SearchableRecyclerView
{

    private lateinit var binding: FragmentUpdatesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var updatesViewModel: UpdatesViewModel
    private lateinit var listAdapter: AndroidXArtifactUpdateListAdapter
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  fragmentJob)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_updates, container, false)
        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application).androidXArtifactUpdateDao
        val viewModelFactory = DashBoardViewModelFactory(dataSource, application)
        updatesViewModel = ViewModelProvider(this, viewModelFactory).get(UpdatesViewModel::class.java)

        listAdapter = AndroidXArtifactUpdateListAdapter(AndroidXArtifactUpdateListAdapter.AndroidXArtifactUpdateListener {
            if(!it.releasePageUrl.isNullOrBlank()) {
                Util.openUrlInWebBrowser(mainActivity, it.releasePageUrl)
            }

        })

        binding.androidXArtifactUpdateList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))
        binding.androidXArtifactUpdateList.adapter = listAdapter
        binding.dashboardViewModel = updatesViewModel
        binding.lifecycleOwner = this

        updatesViewModel.artifactUpdates.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
            listAdapter.notifyDataSetChanged()
            mainActivity.mainCyclicProgressBar.visibility = View.GONE

            if (it.isNotEmpty()) {
                binding.emptyListTextView.visibility = View.GONE
            }
        })

        binding.updatesSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                var list = updatesViewModel.artifactUpdates.value

                list?.let {
                    uiScope.launch {
                        val filteredList = withContext(Dispatchers.Default) {
                            listAdapter.filterBySearchQuery(newText, it)
                        }

                        this@UpdatesFragment.submitListToAdapter(filteredList)
                    }
                }
                return false
            }
        })

        binding.updatesSearchView.setOnCloseListener {
            this@UpdatesFragment.toggleSearch(mainActivity, binding.updatesSearchView, false)
            false
        }

        binding.updatesSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) { } else{
                binding.updatesSearchView.setQuery("", true)
                this@UpdatesFragment.toggleSearch(mainActivity, binding.updatesSearchView, false)
            }
        }

        mainActivity.hideFab()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
    }

    private fun submitListToAdapter (list: MutableList<AndroidXArtifactUpdate>) {
        listAdapter.submitList(list)
        listAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.updates_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_updates_search -> {
                this@UpdatesFragment.toggleSearch(mainActivity, binding.updatesSearchView, true)
                true
            }
            R.id.action_updates_archive -> {
                Util.openUrlInWebBrowser(mainActivity, getString(R.string.jetpack_release_archive_url))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
