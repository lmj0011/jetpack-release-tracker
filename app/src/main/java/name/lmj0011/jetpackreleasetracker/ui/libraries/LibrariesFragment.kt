package name.lmj0011.jetpackreleasetracker.ui.libraries

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentLibrariesBinding
import name.lmj0011.jetpackreleasetracker.helpers.adapters.AndroidXLibraryListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.LibrariesViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView
import name.lmj0011.jetpackreleasetracker.helpers.workers.LibraryRefreshWorker
import name.lmj0011.jetpackreleasetracker.helpers.workers.ProjectSyncAllWorker

class LibrariesFragment : Fragment(R.layout.fragment_libraries), SearchableRecyclerView {

    private lateinit var binding: FragmentLibrariesBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val librariesViewModel by viewModels<LibrariesViewModel> {
        LibrariesViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).androidXArtifactDao,
            requireActivity().application
        )
    }
    private lateinit var listAdapter: AndroidXLibraryListAdapter
    private lateinit var filterMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupAdapter()
        setupRecyclerView()
        setupSearchView()
        setupObservers()
        checkForTestMode()
        setupSwipeToRefresh()
    }

    private fun setupBinding(view: View) {
        binding = FragmentLibrariesBinding.bind(view)
        binding.lifecycleOwner = this
        binding.homeViewModel = librariesViewModel
    }

    private fun setupAdapter() {
        listAdapter = AndroidXLibraryListAdapter(
            AndroidXLibraryListAdapter.AndroidXLibraryListener {},
            AndroidXLibraryListAdapter.AndroidXLibraryStarListener {
                // make a copy since original value can't reliably be modified; ref: https://stackoverflow.com/a/51001329/2445763
                val starredSet = (sharedPreferences.getStringSet(
                    this.getString(R.string.pref_key_starred_libraries),
                    mutableSetOf<String>()
                ) as MutableSet<String>).toMutableSet()

                if (starredSet.contains(it.packageName)) {
                    starredSet.remove(it.packageName)
                } else {
                    starredSet.add(it.packageName)
                }

                sharedPreferences.edit {
                    putStringSet(
                        requireContext().getString(R.string.pref_key_starred_libraries),
                        starredSet
                    )
                    apply()
                    this@LibrariesFragment.refreshListAdapter()
                }
            }
        )
    }

    private fun setupSearchView() {
        binding.librariesSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                this@LibrariesFragment.refreshListAdapter(newText)
                return false
            }
        })

        binding.librariesSearchView.setOnCloseListener {
            this@LibrariesFragment.toggleSearch(
                requireActivity() as MainActivity,
                binding.librariesSearchView,
                false
            )
            false
        }

        binding.librariesSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                binding.librariesSearchView.setQuery("", true)
                this@LibrariesFragment.toggleSearch(
                    requireActivity() as MainActivity,
                    binding.librariesSearchView,
                    false
                )
            }
        }
    }

    private fun setupRecyclerView() {
        binding.androidXLibraryList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        binding.androidXLibraryList.adapter = listAdapter
    }

    private fun setupObservers() {
        librariesViewModel.artifacts.observe(viewLifecycleOwner, Observer {

            // if it's always empty, there's a problem with the network
            if (it.isEmpty()) {
                enqueueNewLibraryRefreshWorkerRequest()
            }

            listAdapter.submitLibArtifacts(it.toList())
            refreshListAdapter()
        })
    }

    private fun checkForTestMode() {
        if (!resources.getBoolean(R.bool.DEBUG_MODE)) {
            binding.testButton.visibility = View.GONE
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            enqueueNewLibraryRefreshWorkerRequest()
            binding.swipeRefresh.isRefreshing = false
            (requireActivity() as MainActivity).showToastMessage(requireContext().getString(R.string.toast_message_updating_libraries))
        }
    }

    private fun enqueueNewLibraryRefreshWorkerRequest() {
        val libraryRefreshWorkerRequest = OneTimeWorkRequestBuilder<LibraryRefreshWorker>()
            .addTag(requireContext().getString(R.string.update_one_time_worker_tag))
            .build()

        WorkManager.getInstance(requireContext())
            .getWorkInfoByIdLiveData(libraryRefreshWorkerRequest.id)
            .observe(viewLifecycleOwner, Observer { workInfo ->
                if (workInfo != null) {
                    val progress = workInfo.progress
                    val value = progress.getInt(ProjectSyncAllWorker.Progress, 0)

                    if (value >= 100) {
                        librariesViewModel.refreshLibraries()
                    }
                }
            })

        WorkManager.getInstance(requireContext()).enqueue(libraryRefreshWorkerRequest)
    }

    private fun refreshListAdapter(query: String? = null) {
        val hasStarredFilter = sharedPreferences.getBoolean(
            requireContext().getString(R.string.pref_key_starred_filter),
            false
        )

        lifecycleScope.launch(Dispatchers.IO) {
            var list = librariesViewModel.getAndroidXLibraryDataset().toMutableList()

            if (hasStarredFilter) {
                list = listAdapter.filterByStarred(requireContext(), list).toMutableList()
            }

            query?.let { str ->
                list = listAdapter.filterBySearchQuery(str, list)
            }

            withContext(Dispatchers.Main) {
                listAdapter.submitList(list)
                listAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.libraries_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        filterMenuItem = menu.findItem(R.id.action_libraries_filter)
        val hasStarredFilter = sharedPreferences.getBoolean(
            requireContext().getString(R.string.pref_key_starred_filter),
            false
        )

        if (hasStarredFilter) {
            filterMenuItem.setIcon(R.drawable.ic_baseline_selected_filter_list_24)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_libraries_search -> {
                this@LibrariesFragment.toggleSearch(
                    requireActivity() as MainActivity,
                    binding.librariesSearchView,
                    true
                )
                true
            }
            R.id.action_libraries_filter -> {
                val hasStarredFilter = sharedPreferences.getBoolean(
                    requireContext().getString(R.string.pref_key_starred_filter),
                    false
                )
                var checkedItem = -1
                if (hasStarredFilter) checkedItem = 0

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filter By")
                    .setSingleChoiceItems(arrayOf("Starred"), checkedItem) { dialog, which ->
                        // Respond to item chosen
                        if (hasStarredFilter) {
                            sharedPreferences.edit().putBoolean(
                                requireContext().getString(R.string.pref_key_starred_filter),
                                false
                            ).apply()
                            filterMenuItem.setIcon(R.drawable.ic_baseline_filter_list_24)
                        } else {
                            sharedPreferences.edit().putBoolean(
                                requireContext().getString(R.string.pref_key_starred_filter),
                                true
                            ).apply()
                            filterMenuItem.setIcon(R.drawable.ic_baseline_selected_filter_list_24)
                        }
                        this@LibrariesFragment.refreshListAdapter()
                        dialog.dismiss()
                    }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
