package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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

class ProjectSyncsFragment : Fragment(R.layout.fragment_project_syncs), SearchableRecyclerView {

    private lateinit var binding: FragmentProjectSyncsBinding
    private val projectSyncsViewModel by viewModels<ProjectSyncsViewModel> {
        ProjectSyncViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).projectSyncDao,
            requireActivity().application
        )
    }
    private lateinit var listAdapter: ProjectSyncListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupAdapter()
        setupObservers()
        setupSearchView()
        setupSwipeToRefresh()
        setupFabWithListener()
    }

    private fun setupBinding(view: View) {
        binding = FragmentProjectSyncsBinding.bind(view)
        binding.projectSyncList.addItemDecoration(
            DividerItemDecoration(
                requireActivity(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.projectSyncsViewModel = projectSyncsViewModel
        binding.lifecycleOwner = this
        binding.learnMoreButton.setOnClickListener {
            Util.openUrlInWebBrowser(
                requireActivity() as MainActivity,
                getString(R.string.project_syncs_learn_more_url)
            )
        }
    }

    private fun setupAdapter() {
        listAdapter = ProjectSyncListAdapter(ProjectSyncListAdapter.ProjectSyncListener {
            val bundle = bundleOf(getString(R.string.key_project_sync_id_bundle_property) to it.id)
            findNavController().navigate(
                R.id.action_navigation_project_syncs_to_editProjectSyncFragment,
                bundle
            )
        })
        binding.projectSyncList.adapter = listAdapter
    }

    private fun setupObservers() {
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
    }

    private fun setupSearchView() {
        binding.projectSyncsSearchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val list = projectSyncsViewModel.projectSyncs.value

                list?.let {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val filteredList = listAdapter.filterBySearchQuery(newText, it)
                        submitListToAdapter(filteredList)
                    }
                }
                return false
            }
        })

        binding.projectSyncsSearchView.setOnCloseListener {
            toggleSearch(requireActivity() as MainActivity, binding.projectSyncsSearchView, false)
            false
        }

        binding.projectSyncsSearchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.projectSyncsSearchView.setQuery("", true)
                toggleSearch(
                    requireActivity() as MainActivity,
                    binding.projectSyncsSearchView,
                    false
                )
            }
        }

    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val projectSyncAllWorkRequest = OneTimeWorkRequestBuilder<ProjectSyncAllWorker>()
                .addTag(requireContext().getString(R.string.project_sync_all_one_time_worker_tag))
                .build()

            WorkManager.getInstance(requireActivity().application)
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

            WorkManager.getInstance(requireActivity().application)
                .enqueue(projectSyncAllWorkRequest)

            binding.swipeRefresh.isRefreshing = false
            (requireActivity() as MainActivity).showToastMessage(requireContext().getString(R.string.toast_message_syncing_projects))
        }
    }

    private fun submitListToAdapter(list: MutableList<ProjectSync>) {
        listAdapter.submitList(list)
        listAdapter.notifyDataSetChanged()
    }

    private fun setupFabWithListener() {
        (requireActivity() as MainActivity).showFabAndSetListener({
            findNavController().navigate(
                ProjectSyncsFragmentDirections.actionNavigationProjectSyncsToCreateProjectSyncFragment()
            )
        }, R.drawable.ic_baseline_add_24)
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
                toggleSearch(
                    requireActivity() as MainActivity,
                    binding.projectSyncsSearchView,
                    true
                )
                true
            }
            R.id.action_updates_archive -> {
                Util.openUrlInWebBrowser(
                    requireActivity() as MainActivity,
                    getString(R.string.jetpack_release_archive_url)
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
