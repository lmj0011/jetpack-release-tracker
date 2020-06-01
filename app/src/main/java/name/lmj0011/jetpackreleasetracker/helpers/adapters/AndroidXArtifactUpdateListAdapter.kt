package name.lmj0011.jetpackreleasetracker.helpers.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.databinding.ListItemAndroidXUpdateBinding
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifactUpdate
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibrary
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class AndroidXArtifactUpdateListAdapter(private val clickListener: AndroidXArtifactUpdateListener):
    ListAdapter<AndroidXArtifactUpdate, AndroidXArtifactUpdateListAdapter.ViewHolder>(AndroidXArtifactUpdateDiffCallback()) {

    class ViewHolder private constructor(val binding: ListItemAndroidXUpdateBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){
        fun bind(clickListener: AndroidXArtifactUpdateListener, update: AndroidXArtifactUpdate) {

            // can't modify the original set ref: https://stackoverflow.com/a/51001329/2445763
            val starredSet = (spf.getStringSet(context.getString(R.string.pref_key_starred_libraries), mutableSetOf<String>()) as MutableSet<String>).toMutableSet()
            binding.update = update
            binding.clickListener = clickListener

            val updateCreatedAtDate = dateFormatter(LocalDateTime.parse(update.createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            if (todayDate == updateCreatedAtDate) {
                binding.dateTextView.text = "Today"
            } else {
                binding.dateTextView.text = updateCreatedAtDate
            }


            LocalDateTime.parse(update.createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            binding.packageNameTextView.text = "${update.name}"
            binding.versionUpdateTextView.text = "${update.previousVersion} -> ${update.latestVersion}"

            if(starredSet.contains(update.packageName)){
                binding.starImageView.visibility = View.VISIBLE
            } else {
                binding.starImageView.visibility = View.GONE
            }

            binding.executePendingBindings()
        }

        companion object {
            lateinit var spf: SharedPreferences
            val todayDate: String = dateFormatter(LocalDateTime.parse(ZonedDateTime.now().toOffsetDateTime().toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))

            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemAndroidXUpdateBinding.inflate(layoutInflater, parent, false)
                spf = PreferenceManager.getDefaultSharedPreferences(parent.context)

                return ViewHolder(binding, parent.context)
            }

            fun dateFormatter(ldt: LocalDateTime): String {
                val month = ldt.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                val dayOfMonth = ldt.dayOfMonth
                val year = ldt.year

                return "$month $dayOfMonth, $year"
            }
        }
    }

    class AndroidXArtifactUpdateDiffCallback : DiffUtil.ItemCallback<AndroidXArtifactUpdate>() {
        override fun areItemsTheSame(oldItem: AndroidXArtifactUpdate, newItem: AndroidXArtifactUpdate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AndroidXArtifactUpdate, newItem: AndroidXArtifactUpdate): Boolean {
            return oldItem == newItem
        }
    }

    class AndroidXArtifactUpdateListener(val clickListener: (update: AndroidXArtifactUpdate) -> Unit) {
        fun onClick(update: AndroidXArtifactUpdate) = clickListener(update)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val update = getItem(position)

        holder.bind(clickListener, update)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    fun filterBySearchQuery(query: String?, list: MutableList<AndroidXArtifactUpdate>): MutableList<AndroidXArtifactUpdate> {
        if (query.isNullOrBlank()) return list

        return list.filter {
            val inName = it.name.contains(query, true)
            val inPackageName = it.packageName.contains(query, true)
            val inPreviousVersion = it.previousVersion.contains(query, true)
            val inLatestVersion = it.latestVersion.contains(query, true)

            return@filter inName || inPackageName || inPreviousVersion || inLatestVersion
        }.toMutableList()
    }
}