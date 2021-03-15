package name.lmj0011.jetpackreleasetracker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_about.view.*
import name.lmj0011.jetpackreleasetracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
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

                R.id.createProjectSyncFragment -> {
                    hideFab()
                }

                R.id.editProjectSyncFragment -> {
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

        v.versionTextView.text = "version: ${BuildConfig.VERSION_NAME}"


        return when (item.itemId) {
            R.id.action_change_theme -> {
                val themeSelections = getThemeSelections()

                MaterialAlertDialogBuilder(this)
                    .setTitle("Change Theme")
                    .setSingleChoiceItems(themeSelections.first, themeSelections.second) { dialog, which ->
                        // Respond to item chosen
                        when(which) {
                            0 -> {
                                sharedPreferences.edit().putInt(
                                    getString(R.string.pref_key_mode_night),
                                    AppCompatDelegate.MODE_NIGHT_NO
                                ).apply()
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            }
                            1 -> {
                                sharedPreferences.edit().putInt(
                                    getString(R.string.pref_key_mode_night),
                                    AppCompatDelegate.MODE_NIGHT_YES
                                ).apply()
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            }
                            2 -> {
                                sharedPreferences.edit().putInt(
                                    getString(R.string.pref_key_mode_night),
                                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                ).apply()
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            }
                        }

                        dialog.dismiss()
                    }
                    .show()
                true
            }
            R.id.action_main_about -> {
                MaterialAlertDialogBuilder(this)
                    .setView(v)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (application as Application).applyTheme()

        recreate()
    }

    /**
     * gets the App theme from preferences, following System theme is default
     *
     * returns a Pair
     *  - first component is the array of choices
     *  - second component is the choice from sharedPrefs
     */
    fun getThemeSelections(): Pair<Array<String>, Int> {
        val modeNight = sharedPreferences.getInt(
            getString(R.string.pref_key_mode_night),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        val themeSelection = when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO ->  {
                if(modeNight != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) 0 else 2
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                if(modeNight != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) 1 else 2
            }
            else -> 2
        }

        return Pair(arrayOf("Light", "Dark", "System"), themeSelection)
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
