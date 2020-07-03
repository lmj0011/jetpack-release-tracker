package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import br.com.simplepass.loadingbutton.presentation.State
import kotlinx.coroutines.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentCreateProjectSyncBinding
import name.lmj0011.jetpackreleasetracker.helpers.Const
import name.lmj0011.jetpackreleasetracker.helpers.factories.ProjectSyncViewModelFactory

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

        binding.pickDepsListTxtImageButton.setOnClickListener { openDepsTxtFile() }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Const.PICK_DEPS_LIST_TXT) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            data?.data?.let {
                binding.depsUrlEditText.setText(it.toString())
            }
        }

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

    private fun openDepsTxtFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }

        startActivityForResult(intent, Const.PICK_DEPS_LIST_TXT)
    }
}
