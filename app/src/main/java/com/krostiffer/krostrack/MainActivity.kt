package com.krostiffer.krostrack

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.krostiffer.krostrack.database.LocationDatabase
import com.krostiffer.krostrack.ui.Help
import com.krostiffer.krostrack.ui.SettingsActivity
import java.util.*


class MainActivity : AppCompatActivity() {

    private val PREFS_FILENAME = "com.krostiffer.krostrack.prefs"
    var prefs: SharedPreferences? = null
    val LEFT_STORE = "left_picker"
    val MIDDLE_STORE = "middle_picker"
    val RIGHT_STORE = "right_picker"
    val MODE_STORE = "run_mode"
    val BUTTON_UID_STORE = "button_uid_store"
    val SELECTED_UID_FROM_DATABASE = "database_uid"

    lateinit var locationDatabase: LocationDatabase

    //create the Database and the layout of the bottom navigation
    override fun onCreate(savedInstanceState: Bundle?) {
        locationDatabase = Room.databaseBuilder(this.applicationContext, LocationDatabase::class.java, "routeDatabase").allowMainThreadQueries().build()
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_FILENAME, 0)
        //Stellt die Sprache auf die gespeicherte ein (Keine schöne Variante, aber ich habe andere nicht zum Laufen bekommen)
        var locale: Locale = Locale.getDefault()
        try {
            if(prefs!!.getString("language", "phone").toString() != "phone"){
                locale = Locale(prefs!!.getString("language", "en").toString())
            }
        } catch (e: Exception){
            locale = Locale.getDefault()
        }
        val res = resources
        val dm = res.displayMetrics
        val conf = res.configuration
        conf.setLocales(LocaleList(locale))
        res.updateConfiguration(conf, dm)

        setContentView(R.layout.activity_main)
        //Das Untere Menü setup-en
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
    //three dot menu Options: Öffnet die Hilfeseite oder die Einstellungen
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuhelp -> {

                startHelp()
                true
            }
            R.id.menusettings ->{

                startSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //startet die Einstellungsactivity
    fun startSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    //startet die Hilfe Activity
    fun startHelp() {
        val intent = Intent(this, Help::class.java)
        startActivity(intent)
    }
}