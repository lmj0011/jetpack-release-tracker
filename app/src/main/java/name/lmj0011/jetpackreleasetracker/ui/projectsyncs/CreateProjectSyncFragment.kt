package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import br.com.simplepass.loadingbutton.presentation.State
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.databinding.FragmentCreateProjectSyncBinding
import name.lmj0011.jetpackreleasetracker.databinding.FragmentProjectSyncsBinding
import name.lmj0011.jetpackreleasetracker.helpers.Util
import name.lmj0011.jetpackreleasetracker.helpers.adapters.ProjectSyncListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.ProjectSyncViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView
import timber.log.Timber

class CreateProjectSyncFragment : Fragment(R.layout.fragment_create_project_sync) {

    private lateinit var binding: FragmentCreateProjectSyncBinding
    private val projectSyncsViewModel by viewModels<ProjectSyncsViewModel> { ProjectSyncViewModelFactory(AppDatabase.getInstance(requireActivity().application).projectSyncDao, requireActivity().application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
    }

    private fun setupBinding(view:View){
        binding = FragmentCreateProjectSyncBinding.bind(view)
        binding.lifecycleOwner = this
        binding.createProjectSyncCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)
    }

    private fun setupObservers(){
        projectSyncsViewModel.errorMessages.observe(viewLifecycleOwner, Observer {
            binding.createProjectSyncCircularProgressButton.revertAnimation()
            binding.createProjectSyncCircularProgressButton.isEnabled = true
            (requireActivity() as MainActivity).showToastMessage(it)
        })

        projectSyncsViewModel.projectSyncs.observe(viewLifecycleOwner, Observer {
            val btnState = binding.createProjectSyncCircularProgressButton.getState()

            // revert button animation and navigate back to Trips
            if (btnState == State.MORPHING || btnState == State.PROGRESS) {
                binding.createProjectSyncCircularProgressButton.revertAnimation()
                binding.createProjectSyncCircularProgressButton.isEnabled = true
                this.findNavController().navigate(R.id.navigation_project_syncs)
            }
        })
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.navigation_project_syncs)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveButtonOnClickListener(v:View) {
        var projectName = binding.projectNameEditText.text.toString()
        var projectDepListUrl = binding.depsUrlEditText.text.toString()

        if(projectName.isBlank()) projectName = "[No Name]"
        if(projectDepListUrl.isBlank()) projectDepListUrl = ""

        projectSyncsViewModel.insertProjectSync(projectName, projectDepListUrl)

        binding.createProjectSyncCircularProgressButton.isEnabled = false
        binding.createProjectSyncCircularProgressButton.startAnimation()

    }
}
