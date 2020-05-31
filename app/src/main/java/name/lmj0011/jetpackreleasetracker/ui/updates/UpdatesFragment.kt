package name.lmj0011.jetpackreleasetracker.ui.updates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.android.synthetic.main.activity_main.*
import name.lmj0011.jetpackreleasetracker.MainActivity
import name.lmj0011.jetpackreleasetracker.R
import name.lmj0011.jetpackreleasetracker.database.AppDatabase
import name.lmj0011.jetpackreleasetracker.databinding.FragmentUpdatesBinding
import name.lmj0011.jetpackreleasetracker.helpers.adapters.AndroidXArtifactUpdateListAdapter
import name.lmj0011.jetpackreleasetracker.helpers.factories.DashBoardViewModelFactory

class UpdatesFragment : Fragment() {

    private lateinit var binding: FragmentUpdatesBinding
    private lateinit var mainActivity: MainActivity
    private lateinit var updatesViewModel: UpdatesViewModel
    private lateinit var listAdapter: AndroidXArtifactUpdateListAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_updates, container, false)
        mainActivity = activity as MainActivity

        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application).androidXArtifactUpdateDao
        val viewModelFactory = DashBoardViewModelFactory(dataSource, application)
        updatesViewModel = ViewModelProvider(this, viewModelFactory).get(UpdatesViewModel::class.java)

        listAdapter = AndroidXArtifactUpdateListAdapter(AndroidXArtifactUpdateListAdapter.AndroidXArtifactUpdateListener {
            if(!it.releasePageUrl.isNullOrBlank()) {
                val webpage: Uri = Uri.parse(it.releasePageUrl)
                val intent = Intent(Intent.ACTION_VIEW, webpage)
                if (intent.resolveActivity(mainActivity.packageManager) != null) {
                    startActivity(intent)
                }
            }

        })

        binding.androidXArtifactUpdateList.addItemDecoration(DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL))
        binding.androidXArtifactUpdateList.adapter = listAdapter
        binding.dashboardViewModel = updatesViewModel
        binding.lifecycleOwner = this

        updatesViewModel.artifactUpdates.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
            listAdapter.notifyDataSetChanged()
            mainActivity.mainCyclicProgressBar.visibility = View.GONE

            if (it.isNotEmpty()) {
                binding.emptyListTextView.visibility = View.GONE
            }
        })

        return binding.root
    }
}
