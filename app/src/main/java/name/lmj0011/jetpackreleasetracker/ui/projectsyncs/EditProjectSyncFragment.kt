package name.lmj0011.jetpackreleasetracker.ui.projectsyncs

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.databinding.FragmentEditProjectSyncBinding
import name.lmj0011.jetpackreleasetracker.helpers.factories.ProjectSyncViewModelFactory

class EditProjectSyncFragment : Fragment(R.layout.fragment_edit_project_sync) {

    private lateinit var binding: FragmentEditProjectSyncBinding
    private val projectSyncsViewModel by viewModels<ProjectSyncsViewModel> {
        ProjectSyncViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).projectSyncDao,
            requireActivity().application
        )
    }
    private var project: ProjectSync? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupAlertDialog()
        setupObservers()
    }

    private fun setupBinding(view: View) {
        binding = FragmentEditProjectSyncBinding.bind(view)
        binding.lifecycleOwner = this
        binding.editProjectSaveCircularProgressButton.setOnClickListener(this::saveButtonOnClickListener)
    }

    private fun setupAlertDialog() {
        binding.editProjectDeleteCircularProgressButton.setOnClickListener { _ ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete this project?")
                .setPositiveButton("Yes") { _, _ ->
                    project?.let { projectSyncsViewModel.deleteProject(it) }
                    findNavController().navigate(R.id.navigation_project_syncs)
                }
                .setNegativeButton("No") { _, _ -> }
                .show()
        }
    }

    private fun setupObservers() {
        projectSyncsViewModel.setProjectSync(requireArguments().getLong(getString(R.string.key_project_sync_id_bundle_property)))

        val dynamicallyAddedDepTextViews = mutableListOf<TextView>()

        projectSyncsViewModel.successMessages.observe(viewLifecycleOwner, Observer {
            (requireActivity() as MainActivity).showToastMessage(it)
        })

        projectSyncsViewModel.errorMessages.observe(viewLifecycleOwner, Observer {
            (requireActivity() as MainActivity).showToastMessage(it)
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
                injectDependencyIntoView(textView, attachToSiblingView, parentLayout, mEntry.key)
                attachToSiblingView = textView
                dynamicallyAddedDepTextViews.add(textView)

                mEntry.value.forEach { artifactStr ->
                    val textView = TextView(context)
                    if (artifactStr.contains("->")) textView.setTextColor(Color.parseColor("#FFE8BB59"))

                    injectDependencyIntoView(
                        textView,
                        attachToSiblingView,
                        parentLayout,
                        "\t\t$artifactStr"
                    )
                    attachToSiblingView = textView
                    dynamicallyAddedDepTextViews.add(textView)
                }
            }
        })
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigate(R.id.navigation_project_syncs)
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

    private fun injectDependencyIntoView(
        view: TextView,
        attachToView: TextView,
        parentLayout: ConstraintLayout,
        depStr: String
    ) {
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

    private fun saveButtonOnClickListener(v: View) {
        project?.let {
            it.name = binding.projectNameEditText.text.toString()
            it.depsListUrl = binding.depsUrlEditText.text.toString()
            it.stableVersionsOnly = binding.stableVersionsOnlySwitch.isChecked

            projectSyncsViewModel.updateProjectSync(it)
        }

        binding.editProjectSaveCircularProgressButton.startAnimation()
        (requireActivity() as MainActivity).hideKeyBoard(binding.editProjectSaveCircularProgressButton)
    }
}
