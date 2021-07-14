package com.krostiffer.krostrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.krostiffer.krostrack.Service.RunNotificationService
import com.krostiffer.krostrack.database.LocationDatabase


class MainActivity : AppCompatActivity() {

    val PREFS_FILENAME = "com.krostiffer.krostrack.prefs"
    var prefs: SharedPreferences? = null
    val LEFT_STORE = "left_picker"
    val MIDDLE_STORE = "middle_picker"
    val RIGHT_STORE = "right_picker"
    val MODE_STORE = "run_mode"
    val BUTTON_UID_STORE = "button_uid_store"
    val SELECTED_UID_FROM_DATABASE = "database_uid"
    val METERS_BEHIND = "distance_behind"
    val SECONDS_BEHIND = "time_behind"

    lateinit var locationDatabase: LocationDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        locationDatabase = Room.databaseBuilder(this.applicationContext, LocationDatabase::class.java, "routeDatabase").allowMainThreadQueries().build()
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_FILENAME, 0)
        setContentView(R.layout.activity_main)


        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_run, R.id.navigation_routes
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.dotmenu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuhelp -> {
                Toast.makeText(applicationContext, "click on help", Toast.LENGTH_LONG).show()
                //loadFragment(SettingsFragment())
                loadFragment(SettingsFragment())
                true
            }
            R.id.menusettings ->{
                Toast.makeText(applicationContext, "click on settings", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.attach(fragment)
        transaction.setReorderingAllowed(true)
        transaction.addToBackStack("")

        transaction.commit()
    }




}