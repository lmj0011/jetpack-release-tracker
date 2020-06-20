package name.lmj0011.jetpackreleasetracker.helpers.adapters

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.databinding.ListItemLibraryBinding
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibrary
import timber.log.Timber

class AndroidXLibraryListAdapter(
    private val clickListener: AndroidXLibraryListener,
    private val starClickListener: AndroidXLibraryStarListener
):
    ListAdapter<AndroidXLibrary, AndroidXLibraryListAdapter.ViewHolder>(AndroidXLibraryDiffCallback()) {

    private var libArtifacts: List<AndroidXArtifact> = listOf()

    class ViewHolder private constructor(val binding: ListItemLibraryBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){

        // Views previously created dynamically for this View Holder
        // remove these Views before recycling this View Holder
        private val dynamicallyAddedViews = mutableListOf<View>()

        fun bind(clickListener: AndroidXLibraryListener, starClickListener: AndroidXLibraryStarListener, library: AndroidXLibrary, artifacts: List<AndroidXArtifact>) {
            binding.packageNameTextView.text = library.packageName
            binding.library = library
            binding.starClickListener = starClickListener

            val starredSet = getStarredSet(context).toMutableSet()
            val parentLayout = binding.listItemLibraryConstraintLayout
            var attachToViewForArtifact = binding.artifactTextView
            var attachToViewForLatestStable = binding.stableVersionTextView
            var attachToViewForLatestRelease = binding.latestReleaseTextView


            if(starredSet.contains(library.packageName)){
                binding.starImageView.setBackgroundResource(R.drawable.ic_star_yellow_24dp)
            } else {
                binding.starImageView.setBackgroundResource(R.drawable.ic_star_border_black_24dp)
            }

            dynamicallyAddedViews.forEach { parentLayout.removeView(it) }

            library.artifactNames.forEach { artifactName ->
                val artifact = artifacts.find {
                    it.name == "${library.packageName}:$artifactName"
                }

                val artifactTextView = TextView(context)
                addArtifactToView(artifactTextView, attachToViewForArtifact, parentLayout, binding, artifact)
                attachToViewForArtifact = artifactTextView // the View that we attach to the bottom of in the Constraint Layout; it's the previous one added
                dynamicallyAddedViews.add(artifactTextView)

                val stableVersionTextView = TextView(context)
                addArtifactStableVersionToView(stableVersionTextView, attachToViewForLatestStable, parentLayout, binding, artifact)
                attachToViewForLatestStable = stableVersionTextView
                dynamicallyAddedViews.add(stableVersionTextView)

                val latestVersionTextView = TextView(context)
                addArtifactLatestVersionToView(latestVersionTextView, attachToViewForLatestRelease, parentLayout, binding, artifact)
                attachToViewForLatestRelease = latestVersionTextView
                dynamicallyAddedViews.add(latestVersionTextView)
            }

            binding.executePendingBindings()
        }

        private fun addArtifactToView(view: TextView, attachToView: TextView, parentLayout: ConstraintLayout, binding: ListItemLibraryBinding, artifact: AndroidXArtifact?) {
            artifact?.let {
                val c = ConstraintSet()

                view.id = View.generateViewId()
                view.text = it.name.split(":")[1]
                view.maxEms = 12
                view.isSingleLine = true
                view.movementMethod = ScrollingMovementMethod()
                parentLayout.addView(view)
                c.clone(parentLayout)
                c.connect(view.id, ConstraintSet.TOP, attachToView.id, ConstraintSet.BOTTOM)
                c.connect(view.id, ConstraintSet.LEFT, view.rootView.id, ConstraintSet.LEFT)
                c.applyTo(parentLayout)
            }
        }

        private fun addArtifactStableVersionToView(view: TextView, attachToView: TextView, parentLayout: ConstraintLayout, binding: ListItemLibraryBinding, artifact: AndroidXArtifact?) {
            artifact?.let {
                val c = ConstraintSet()

                view.id = View.generateViewId()
                view.text = "-"
                if (it.latestStableVersion.isNotBlank()) {
                    view.text = it.latestStableVersion
                }
                view.maxEms = 8
                view.isSingleLine = true
                view.movementMethod = ScrollingMovementMethod()
                parentLayout.addView(view)
                c.clone(parentLayout)
                c.connect(view.id, ConstraintSet.TOP, attachToView.id, ConstraintSet.BOTTOM)
                c.connect(view.id, ConstraintSet.LEFT, binding.leftVerticalGuideline.id, ConstraintSet.RIGHT)
                c.applyTo(parentLayout)
            }
        }

        private fun addArtifactLatestVersionToView(view: TextView, attachToView: TextView, parentLayout: ConstraintLayout, binding: ListItemLibraryBinding, artifact: AndroidXArtifact?) {
            artifact?.let {
                val c = ConstraintSet()

                view.id = View.generateViewId()
                view.text = "-"
                if (it.latestVersion.isNotBlank()) {
                    view.text = it.latestVersion
                }
                view.maxEms = 8
                view.isSingleLine = true
                view.movementMethod = ScrollingMovementMethod()
                parentLayout.addView(view)
                c.clone(parentLayout)
                c.connect(view.id, ConstraintSet.TOP, attachToView.id, ConstraintSet.BOTTOM)
                c.connect(view.id, ConstraintSet.LEFT, binding.rightVerticalGuideline.id, ConstraintSet.RIGHT)
                c.applyTo(parentLayout)
            }
        }

        companion object {
            fun getStarredSet(context: Context): MutableSet<String> {
                val spf = PreferenceManager.getDefaultSharedPreferences(context)
                return (spf.getStringSet(context.getString(R.string.pref_key_starred_libraries), mutableSetOf<String>()) as MutableSet<String>).toMutableSet()
            }

            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemLibraryBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class AndroidXLibraryDiffCallback : DiffUtil.ItemCallback<AndroidXLibrary>() {
        override fun areItemsTheSame(oldItem: AndroidXLibrary, newItem: AndroidXLibrary): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: AndroidXLibrary, newItem: AndroidXLibrary): Boolean {
            return oldItem == newItem
        }
    }

    class AndroidXLibraryListener(val clickListener: (library: AndroidXLibrary) -> Unit) {
        fun onClick(library: AndroidXLibrary) = clickListener(library)
    }

    class AndroidXLibraryStarListener(val clickListener: (library: AndroidXLibrary) -> Unit) {
        fun onClick(library: AndroidXLibrary) = clickListener(library)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val library = getItem(position)

        holder.bind(clickListener, starClickListener, library, libArtifacts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    fun  submitLibArtifacts(artifacts: List<AndroidXArtifact>) {
        libArtifacts = artifacts
    }

    fun filterBySearchQuery(query: String?, list: MutableList<AndroidXLibrary>): MutableList<AndroidXLibrary> {
        if (query.isNullOrBlank()) return list

        return list.filter {
            val inPackageName = it.packageName.contains(query, true)
            val inArtifact = it.artifactNames.any { name ->
                val artifact = libArtifacts.find { artifact -> artifact.name == "${it.packageName}:${name}" }

                if (artifact == null) {
                    false
                } else {
                    artifact.name.contains(query, true) ||
                    artifact.latestStableVersion.contains(query, true) ||
                    artifact.latestVersion.contains(query, true)
                }
            }
            return@filter inPackageName || inArtifact
        }.toMutableList()
    }

    fun filterByStarred(context: Context, list: List<AndroidXLibrary>): List<AndroidXLibrary> {
        val starredSet = ViewHolder.getStarredSet(context).toMutableSet()

        return list.filter {
            starredSet.contains(it.packageName)
        }.toMutableList()
    }
}