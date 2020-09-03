package name.lmj0011.jetpackreleasetracker.ui.updates

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdate
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentUpdatesBinding
import name.lmj0011.jetpackreleasetracker.helpers.Util
import name.lmj0011.jetpackreleasetracker.helpers.adapters.AndroidXArtifactUpdateListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.DashBoardViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView

class UpdatesFragment : Fragment(R.layout.fragment_updates), SearchableRecyclerView {

    private lateinit var binding: FragmentUpdatesBinding
    private val updatesViewModel by viewModels<UpdatesViewModel> { DashBoardViewModelFactory(AppDatabase.getInstance(requireActivity().application).androidXArtifactUpdateDao, requireActivity().application) }
    private lateinit var listAdapter: AndroidXArtifactUpdateListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupAdapter()
        setupObservers()
        setupSearchView()
    }

    private fun setupBinding(view:View){
        binding = FragmentUpdatesBinding.bind(view)
        binding.androidXArtifactUpdateList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.dashboardViewModel = updatesViewModel
        binding.lifecycleOwner = this
    }

    private fun setupAdapter(){
        listAdapter = AndroidXArtifactUpdateListAdapter(AndroidXArtifactUpdateListAdapter.AndroidXArtifactUpdateListener {
            if(!it.releasePageUrl.isBlank()) {
                Util.openUrlInWebBrowser(requireActivity() as MainActivity, it.releasePageUrl)
            }

        })
        binding.androidXArtifactUpdateList.adapter = listAdapter
    }

    private fun setupObservers(){
        updatesViewModel.artifactUpdates.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
            listAdapter.notifyDataSetChanged()

            if (it.isNotEmpty()) {
                binding.emptyListTextView.visibility = View.GONE
            }
        })
    }

    private fun setupSearchView(){
        binding.updatesSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val list = updatesViewModel.artifactUpdates.value

                list?.let {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val filteredList = listAdapter.filterBySearchQuery(newText, it)
                        submitListToAdapter(filteredList)
                    }
                }
                return false
            }
        })

        binding.updatesSearchView.setOnCloseListener {
            toggleSearch(requireActivity() as MainActivity, binding.updatesSearchView, false)
            false
        }

        binding.updatesSearchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
            if(!hasFocus){
                binding.updatesSearchView.setQuery("", true)
                toggleSearch(requireActivity() as MainActivity, binding.updatesSearchView, false)
            }
        }
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
                toggleSearch(requireActivity() as MainActivity, binding.updatesSearchView, true)
                true
            }
            R.id.action_updates_archive -> {
                Util.openUrlInWebBrowser(requireActivity() as MainActivity, getString(R.string.jetpack_release_archive_url))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
