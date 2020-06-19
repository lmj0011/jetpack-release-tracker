package name.lmj0011.jetpackreleasetracker.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.ProjectSync
import name.lmj0011.jetpackreleasetracker.databinding.ListItemProjectSyncBinding

class ProjectSyncListAdapter(private val clickListener: ProjectSyncListener):
    ListAdapter<ProjectSync, ProjectSyncListAdapter.ViewHolder>(ProjectSyncDiffCallback()) {

    class ViewHolder private constructor(val binding: ListItemProjectSyncBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){
        fun bind(clickListener: ProjectSyncListener, projectSync: ProjectSync) {
            binding.projectSync = projectSync
            binding.clickListener = clickListener

            binding.projectNameTextView.text = projectSync.name

            when {
                // all deps are up to date
                (projectSync.upToDateCount > 0 && projectSync.outdatedCount == 0)  -> {
                    binding.syncIndicatorImageView.setImageResource(R.drawable.ic_baseline_sync_green_24)
                    binding.syncStatusTextView.text = context.getString(R.string.project_sync_all_up_to_date)
                }
                (projectSync.outdatedCount == 1)  -> {
                    binding.syncIndicatorImageView.setImageResource(R.drawable.ic_baseline_sync_problem_24)
                    binding.syncStatusTextView.text = context.getString(R.string.project_sync_outdated_singular)
                }
                (projectSync.outdatedCount > 1)  -> {
                    binding.syncIndicatorImageView.setImageResource(R.drawable.ic_baseline_sync_problem_24)
                    binding.syncStatusTextView.text = context.getString(R.string.project_sync_outdated_plural, projectSync.outdatedCount)
                }
                else -> {
                    binding.syncIndicatorImageView.setImageResource(R.drawable.ic_baseline_sync_24)
                    binding.syncStatusTextView.text = context.getString(R.string.project_sync_no_dependencies)
                }
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemProjectSyncBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }

        }
    }

    class ProjectSyncDiffCallback : DiffUtil.ItemCallback<ProjectSync>() {
        override fun areItemsTheSame(oldItem: ProjectSync, newItem: ProjectSync): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProjectSync, newItem: ProjectSync): Boolean {
            return oldItem == newItem
        }
    }

    class ProjectSyncListener(val clickListener: (update: ProjectSync) -> Unit) {
        fun onClick(update: ProjectSync) = clickListener(update)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val update = getItem(position)

        holder.bind(clickListener, update)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    fun filterBySearchQuery(query: String?, list: MutableList<ProjectSync>): MutableList<ProjectSync> {
        if (query.isNullOrBlank()) return list

        return list.filter {
            val inName = it.name.contains(query, true)

            return@filter inName
        }.toMutableList()
    }
}