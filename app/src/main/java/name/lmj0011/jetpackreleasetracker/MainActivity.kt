package name.lmj0011.jetpackreleasetracker

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_about.view.*
import name.lmj0011.jetpackreleasetracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_libraries, R.id.navigation_updates, R.id.navigation_project_syncs ,R.id.navigation_notifications)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        setupNavigationListener()
    }

    private fun setupNavigationListener(){
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id){
                 R.id.navigation_libraries -> {
                    hideFab()
                }

                R.id.navigation_updates -> {
                    hideFab()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        intent.extras?.getInt("menuItemId")?.let {
            navigateTo(it)
        }

        intent.replaceExtras(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)

        if(!resources.getBoolean(R.bool.DEBUG_MODE)) {
            v.versionTextView.text = "v${BuildConfig.VERSION_NAME} (${resources.getString(R.string.app_build)})"
        } else {
            v.versionTextView.text = "v${BuildConfig.VERSION_NAME} (${resources.getString(R.string.app_build)}) DEBUG"
        }


        return when (item.itemId) {
            R.id.action_main_about -> {
                MaterialAlertDialogBuilder(this)
                    .setView(v)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showToastMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        val toast = Toast.makeText(this, message, duration)
        toast.setGravity(Gravity.TOP, 0, 150)
        toast.show()
    }

    fun showFabAndSetListener(cb: () -> Unit, imgSrcId: Int) {
        binding.fab.let {
            it.setOnClickListener(null) // should remove all attached listeners
            it.setOnClickListener { cb() }

            // hide and show to repaint the img src
            it.hide()

            it.setImageResource(imgSrcId)

            it.show()
        }
    }

    fun hideFab() {
        binding.fab.hide()
    }

    fun showKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun navigateTo(id: Int) {
        when (id) {
            R.id.navigation_libraries -> {
                navController.navigate(id)
            }
            R.id.navigation_updates -> {
                navController.navigate(id)
            }
        }
    }
}
