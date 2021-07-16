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

    private val PREFS_FILENAME = "com.krostiffer.krostrack.prefs"
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

    //create the Database and the layout of the bottom navigation
    override fun onCreate(savedInstanceState: Bundle?) {
        locationDatabase = Room.databaseBuilder(this.applicationContext, LocationDatabase::class.java, "routeDatabase").allowMainThreadQueries().build()
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_FILENAME, 0)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_run, R.id.navigation_routes //Unteres Menü aus RUN und HISTORY
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }
    //three dot menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dotmenu, menu)
        return true
    }
    //three dot menu Options (currently does nothing but making a Toast notification)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuhelp -> {
                Toast.makeText(applicationContext, "click on help", Toast.LENGTH_LONG).show()
                true
            }
            R.id.menusettings ->{
                Toast.makeText(applicationContext, "click on settings", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }




}