package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
import name.lmj0011.jetpackreleasetracker.databinding.FragmentEditProjectSyncBinding
import name.lmj0011.jetpackreleasetracker.databinding.FragmentProjectSyncsBinding
import name.lmj0011.jetpackreleasetracker.helpers.Util
import name.lmj0011.jetpackreleasetracker.helpers.adapters.ProjectSyncListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.ProjectSyncViewModelFactory
import name.lmj0011.jetpackreleasetracker.helpers.interfaces.SearchableRecyclerView
import timber.log.Timber

class EditProjectSyncFragment : Fragment()
{

    private lateinit var binding: FragmentEditProjectSyncBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var projectSyncsViewModel: ProjectSyncsViewModel
    private var project: ProjectSync? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_project_sync, container, false)
        mainActivity = activity as MainActivity
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application).projectSyncDao
        val viewModelFactory = ProjectSyncViewModelFactory(dataSource, application)
        val dynamicallyAddedDepTextViews = mutableListOf<TextView>()
        projectSyncsViewModel = ViewModelProvider(this, viewModelFactory).get(ProjectSyncsViewModel::class.java)

        binding.lifecycleOwner = this
        
        binding.editProjectSaveCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)
        
        binding.editProjectDeleteCircularProgressButton.setOnClickListener { _ ->
            project?.let { projectSyncsViewModel.deleteProject(it) }
            findNavController().navigate(R.id.navigation_project_syncs)
        }

        projectSyncsViewModel.successMessages.observe(viewLifecycleOwner, Observer {
            mainActivity.showToastMessage(it)
        })

        projectSyncsViewModel.errorMessages.observe(viewLifecycleOwner, Observer {
            mainActivity.showToastMessage(it)
        })

        projectSyncsViewModel.projectSync.observe(viewLifecycleOwner, Observer {
            project = it
            injectProjectIntoView(it)
            binding.editProjectSaveCircularProgressButton.stopAnimation()
        })

        projectSyncsViewModel.projectDepsMap.observe(viewLifecycleOwner, Observer {
            val parentLayout = binding.editProjectSyncNestedConstraintLayout1
            var attachToSiblingView = binding.depsListHeaderTextView

            // clears out the previous list
            dynamicallyAddedDepTextViews.forEach { v -> parentLayout.removeView(v) }

            it.forEach { mEntry ->
                val textView = TextView(context)
                textView.setTypeface(null, Typeface.BOLD)
                injectDependencyIntoView(textView, attachToSiblingView, parentLayout, "${mEntry.key}")
                attachToSiblingView = textView
                dynamicallyAddedDepTextViews.add(textView)

                mEntry.value.forEach { artifactStr ->
                    val textView = TextView(context)
                    if(artifactStr.contains("->"))  textView.setTextColor(Color.parseColor("#FFE8BB59"))

                    injectDependencyIntoView(textView, attachToSiblingView, parentLayout, "\t\t$artifactStr")
                    attachToSiblingView = textView
                    dynamicallyAddedDepTextViews.add(textView)
                }
            }
        })

        projectSyncsViewModel.setProjectSync(requireArguments().getLong(getString(R.string.key_project_sync_id_bundle_property)))

        mainActivity.hideFab()

        return binding.root
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

    private fun injectProjectIntoView(_project: ProjectSync?) {
        _project?.let {
            binding.projectNameEditText.setText(it.name)
            binding.depsUrlEditText.setText(it.depsListUrl)
            binding.stableVersionsOnlySwitch.isChecked = it.stableVersionsOnly
        }
    }

    private fun injectDependencyIntoView(view: TextView, attachToView: TextView, parentLayout: ConstraintLayout, depStr: String) {
        val c = ConstraintSet()

        view.id = View.generateViewId()
        view.text = depStr
        view.width = parentLayout.width
        view.maxEms = 8
        view.isSingleLine = true
        view.movementMethod = ScrollingMovementMethod()
        parentLayout.addView(view)
        c.clone(parentLayout)
        c.connect(view.id, ConstraintSet.TOP, attachToView.id, ConstraintSet.BOTTOM)
        c.connect(view.id, ConstraintSet.LEFT, parentLayout.id, ConstraintSet.LEFT)
        c.connect(view.id, ConstraintSet.RIGHT, parentLayout.id, ConstraintSet.RIGHT)
        c.applyTo(parentLayout)
    }

    private fun saveButtonOnClickListener(v: View? = null) {
        project?.let {
            it.name = binding.projectNameEditText.text.toString()
            it.depsListUrl = binding.depsUrlEditText.text.toString()
            it.stableVersionsOnly = binding.stableVersionsOnlySwitch.isChecked

            projectSyncsViewModel.updateProjectSync(it)
        }

        binding.editProjectSaveCircularProgressButton.startAnimation()
        mainActivity.hideKeyBoard(binding.editProjectSaveCircularProgressButton)
    }
}
