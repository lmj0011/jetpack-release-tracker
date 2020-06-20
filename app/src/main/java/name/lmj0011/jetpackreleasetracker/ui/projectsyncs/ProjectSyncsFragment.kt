package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.databinding.FragmentProjectSyncsBinding
import name.lmj0011.jetpackreleasetracker.helpers.Util
import name.lmj0011.jetpackreleasetracker.helpers.adapters.ProjectSyncListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.ProjectSyncViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView
import name.lmj0011.jetpackreleasetracker.helpers.workers.ProjectSyncAllWorker
import name.lmj0011.jetpackreleasetracker.helpers.workers.ProjectSyncAllWorker.Companion.Progress
import timber.log.Timber

class ProjectSyncsFragment : Fragment(),
    SearchableRecyclerView
{

    private lateinit var binding: FragmentProjectSyncsBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var projectSyncsViewModel: ProjectSyncsViewModel
    private lateinit var listAdapter: ProjectSyncListAdapter
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  fragmentJob)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_project_syncs, container, false)
        mainActivity = activity as MainActivity
//        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application).projectSyncDao
        val viewModelFactory = ProjectSyncViewModelFactory(dataSource, application)
        projectSyncsViewModel = ViewModelProvider(this, viewModelFactory).get(ProjectSyncsViewModel::class.java)

        listAdapter = ProjectSyncListAdapter(ProjectSyncListAdapter.ProjectSyncListener {
            val bundle = bundleOf(getString(R.string.key_project_sync_id_bundle_property) to it.id)
            findNavController().navigate(
                R.id.action_navigation_project_syncs_to_editProjectSyncFragment,
                bundle
            )
        })

        binding.projectSyncList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))
        binding.projectSyncList.adapter = listAdapter
        binding.projectSyncsViewModel = projectSyncsViewModel
        binding.lifecycleOwner = this

        projectSyncsViewModel.projectSyncs.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
            listAdapter.notifyDataSetChanged()

            if (it.isEmpty()) {
                binding.swipeRefresh.visibility = View.GONE
                binding.emptyListContainer.visibility = View.VISIBLE
            } else {
                binding.swipeRefresh.visibility = View.VISIBLE
                binding.emptyListContainer.visibility = View.GONE
            }
        })

        binding.learnMoreButton.setOnClickListener {
            Util.openUrlInWebBrowser(mainActivity, getString(R.string.project_syncs_learn_more_url))
        }


        binding.projectSyncsSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                var list = projectSyncsViewModel.projectSyncs.value

                list?.let {
                    uiScope.launch {
                        val filteredList = withContext(Dispatchers.Default) {
                            listAdapter.filterBySearchQuery(newText, it)
                        }

                        this@ProjectSyncsFragment.submitListToAdapter(filteredList)
                    }
                }
                return false
            }
        })

        binding.projectSyncsSearchView.setOnCloseListener {
            this@ProjectSyncsFragment.toggleSearch(mainActivity, binding.projectSyncsSearchView, false)
            false
        }

        binding.projectSyncsSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (hasFocus) { } else{
                binding.projectSyncsSearchView.setQuery("", true)
                this@ProjectSyncsFragment.toggleSearch(mainActivity, binding.projectSyncsSearchView, false)
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            val projectSyncAllWorkRequest = OneTimeWorkRequestBuilder<ProjectSyncAllWorker>().build()

            WorkManager.getInstance(application)
                .getWorkInfoByIdLiveData(projectSyncAllWorkRequest.id)
                .observe(viewLifecycleOwner, Observer { workInfo ->
                    if (workInfo != null) {
                        val progress = workInfo.progress
                        val value = progress.getInt(Progress, 0)

                        if (value >= 100) {
                            projectSyncsViewModel.refreshProjectSyncs()
                        }
                    }
                })

            WorkManager.getInstance(application).enqueue(projectSyncAllWorkRequest)

            binding.swipeRefresh.isRefreshing = false
        }

        mainActivity.showFabAndSetListener({
            findNavController().navigate(
                ProjectSyncsFragmentDirections.actionNavigationProjectSyncsToCreateProjectSyncFragment()
            )
        }, R.drawable.ic_baseline_add_24)

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
    }

    private fun submitListToAdapter (list: MutableList<ProjectSync>) {
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
                this@ProjectSyncsFragment.toggleSearch(mainActivity, binding.projectSyncsSearchView, true)
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
