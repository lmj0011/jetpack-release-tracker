package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import br.com.simplepass.loadingbutton.presentation.State
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
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

class CreateProjectSyncFragment : Fragment()
{

    private lateinit var binding: FragmentCreateProjectSyncBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var projectSyncsViewModel: ProjectSyncsViewModel
    private var fragmentJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main +  fragmentJob)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_project_sync, container, false)
        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application).projectSyncDao
        val viewModelFactory = ProjectSyncViewModelFactory(dataSource, application)
        projectSyncsViewModel = ViewModelProvider(this, viewModelFactory).get(ProjectSyncsViewModel::class.java)

        binding.lifecycleOwner = this
        binding.createProjectSyncCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)

        projectSyncsViewModel.errorMessages.observe(viewLifecycleOwner, Observer {
            binding.createProjectSyncCircularProgressButton.revertAnimation()
            binding.createProjectSyncCircularProgressButton.isEnabled = true
            mainActivity.showToastMessage(it)
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

        mainActivity.hideFab()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.navigation_project_syncs)
                true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveButtonOnClickListener(v: View) {
        var projectName = binding.projectNameEditText.text.toString()
        var projectDepListUrl = binding.depsUrlEditText.text.toString()

        if(projectName.isNullOrBlank()) projectName = "[No Name]"
        if(projectDepListUrl.isNullOrBlank()) projectDepListUrl = ""

        projectSyncsViewModel.insertProjectSync(projectName, projectDepListUrl)

        binding.createProjectSyncCircularProgressButton.isEnabled = false
        binding.createProjectSyncCircularProgressButton.startAnimation()

    }
}
