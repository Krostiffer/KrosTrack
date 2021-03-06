package com.krostiffer.krostrack.ui.run

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.dynamic.SupportFragmentWrapper
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.krostiffer.krostrack.*
import com.krostiffer.krostrack.Service.RunNotificationService
import com.krostiffer.krostrack.database.LocationDao
import com.krostiffer.krostrack.database.LocationDatabase
import com.krostiffer.krostrack.database.LocationExt
import com.krostiffer.krostrack.ui.run.modes.JustRunFragment
import com.krostiffer.krostrack.ui.run.modes.SpeedSetting
import com.krostiffer.krostrack.ui.run.modes.VsYourselfFragment
import java.text.DateFormat.getDateTimeInstance


class RunFragment : Fragment() {

    private lateinit var runNotificationServiceIntent: Intent
    lateinit var rnService: RunNotificationService
    private lateinit var servconn: ServiceConnection
    var isBound = false
    val nrOfMenus = 3 //set to 4 if new mode
    private val FINE_REQUEST_CODE = 101
    private val BACKGROUND_REQUEST_CODE = 102
    lateinit var mainHandler: Handler



    private inner class PagerAdapter(f:FragmentActivity) : FragmentStateAdapter(f) { //Erstellt den Inhalt des Tablayout der entsprechenden Modi
        var flist = listOf(SpeedSetting(), VsYourselfFragment(), JustRunFragment()) //Nach Zeit, Nach Route, Einfach so

        override fun getItemCount(): Int {
            return nrOfMenus
        }

        override fun createFragment(position: Int): Fragment {
            return flist[position]
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val mainAct: MainActivity = activity as MainActivity
        mainHandler = Handler(Looper.getMainLooper())
        val locationDatabase: LocationDatabase = mainAct.locationDatabase
        val locationDao: LocationDao = locationDatabase.locationDao()
        if (mainAct.ServiceisBound) {
            //Die LocationList des Services wird ??bernommen und der Service beendet und unbinded
            val locList = rnService.locList
            rnService.killService()
            mainAct.unbindService(servconn)
            mainAct.ServiceisBound = false

            if (locList.isNotEmpty() && locList.size > 1) { //sobald mindestens zwei Locations erfasst wurde, wird die Route in der Datenbank gespeichert
                val l = addToDatabase(locList, locationDao)
                val intent = Intent(mainAct, ShowMaps::class.java)
                intent.putExtra("lat", l[0])
                intent.putExtra("lon", l[1])
                intent.putExtra("spe", l[2])
                intent.putExtra("tim", l[3])
                startActivity(intent)
            } else {
                Toast.makeText(mainAct, getString(R.string.track_too_short_warning), Toast.LENGTH_LONG).show() //wenn weniger als zwei Positionen erfasst wurden, dann wird eine Nachricht angezeigt, dass die Strecke zu kurz war
            }
            super.onDestroy()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        prefResetIfNotSet()
        val root = inflater.inflate(R.layout.fragment_run, container, false)
        val mainAct: MainActivity = activity as MainActivity
        val tabLayout = root.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = root.findViewById<ViewPager2>(R.id.viewPager)
        changePref(mainAct.SELECTED_UID_FROM_DATABASE, -1)
        val locationDatabase: LocationDatabase = mainAct.locationDatabase
        val locationDao: LocationDao = locationDatabase.locationDao()
        viewPager.adapter = PagerAdapter(f = mainAct)
        viewPager.currentItem = mainAct.prefs!!.getInt(mainAct.MODE_STORE, 0)
        //Tab icons werden gesetzt (modus 3 wird momentan nie erreicht, ist f??r die Zukunft da)
        TabLayoutMediator(tabLayout, viewPager) {tab, position ->
            if(position == 0) {
                tab.icon = ContextCompat.getDrawable(mainAct, R.drawable.speedometer)
            }
            if(position == 1) {
                tab.icon = ContextCompat.getDrawable(mainAct, R.drawable.run_vs_self_icon)
            }

            if(position == 2) {
                tab.icon = ContextCompat.getDrawable(mainAct, R.drawable.run)
            }
            if(position == 3) {
                tab.icon = ContextCompat.getDrawable(mainAct, R.drawable.run)
            }

        }.attach()
        val vsModeChange = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                changePref(mainAct.MODE_STORE, position)
            }
        }
        viewPager.registerOnPageChangeCallback(vsModeChange)

        //Connection zum Service
        servconn = object : ServiceConnection {
            override fun onServiceConnected(
                className: ComponentName,
                service: IBinder,
            ) {
                val binder = service as RunNotificationService.RnsLocalBinder
                rnService = binder.getService()
                mainAct.ServiceisBound = true
            }



            override fun onServiceDisconnected(name: ComponentName) {
                Log.println(Log.ASSERT,"ServiceConnection", "Service Disconnected")
                mainAct.ServiceisBound = false
            }
        }
        //Location ??berpr??fung, Background Location k??nnte unn??tig sein
        if(ActivityCompat.checkSelfPermission(mainAct, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(mainAct, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(mainAct, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(mainAct, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), BACKGROUND_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(mainAct, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), FINE_REQUEST_CODE)
            }
        }

        val startButton: Button = root.findViewById(R.id.startbutton)
        val stopButton: Button = root.findViewById(R.id.stopbutton)
        val laufMaps: androidx.fragment.app.FragmentContainerView = root.findViewById(R.id.livemap)
        //POSITION AUF MAINACT PACKEN

        if(!mainAct.ServiceisBound) {
            laufMaps.visibility = GONE
            stopButton.visibility = GONE
            startButton.visibility = VISIBLE
        } else {
            laufMaps.visibility = VISIBLE
            stopButton.visibility = VISIBLE
            startButton.visibility = GONE
        }


        startButton.setOnClickListener{
            Log.println(Log.ASSERT, "Button Press", "Service starting...")
            var start = true
            //??berpr??fen ob das Ger??t ??berhaupt Location aktiviert hat, wenn nein, wird das Starten unterbunden und eine entsprechende Nachricht angezeigt
            val locationManager = mainAct.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isloc = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if(!isloc){
                val builder = AlertDialog.Builder(mainAct)
                builder.setTitle(R.string.location_not_enabled)
                builder.setMessage(R.string.turn_location_on)
                builder.show()
                start = false
            }
            when(mainAct.prefs!!.getInt(mainAct.MODE_STORE, -1)) {
                -1 -> {
                    Log.println(Log.ASSERT, "Button Press", "Error")
                }
                0 -> { //Beim Modus 0 (feste Geschwindigkeit) wird die eingestellte Geschwindigkeit ??bergeben
                    runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                        intent.putExtra("com.krostiffer.krostrack.mode", "just speed")
                        intent.putExtra("com.krostiffer.krostrack.left", mainAct.prefs!!.getInt(mainAct.LEFT_STORE, -1))
                        intent.putExtra("com.krostiffer.krostrack.middle", mainAct.prefs!!.getInt(mainAct.MIDDLE_STORE, -1))
                        intent.putExtra("com.krostiffer.krostrack.right", mainAct.prefs!!.getInt(mainAct.RIGHT_STORE, -1))
                        intent.putExtra("com.krostiffer.krostrack.timebehind", mainAct.prefs!!.getFloat("TimeBehind", -1.0f))
                    }
                }
                1 -> { //Beim Modus 0 (gegen eine Aufzeichnung laufen) wird die angeklickte Aufzeichnung ??bergeben. die dbid wird in VsYourselfFragment gesetzt durch anklicken der Route
                    Log.println(Log.ASSERT, "Button Press", "Mode 1")
                    val dbid = mainAct.prefs!!.getInt(mainAct.SELECTED_UID_FROM_DATABASE, -1)
                    if(dbid > 0) {
                        val route = mainAct.locationDatabase.locationDao().getRoute(dbid)
                        if(route.latitudes.isNotEmpty()) {
                            runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                                    intent.putExtra("com.krostiffer.krostrack.mode", "run vs self")
                                    intent.putExtra("com.krostiffer.krostrack.speeds", route.speeds)
                                    intent.putExtra("com.krostiffer.krostrack.times", route.times)
                                    intent.putExtra("com.krostiffer.krostrack.latitudes", route.latitudes)
                                    intent.putExtra("com.krostiffer.krostrack.longitudes", route.longitudes)
                                    intent.putExtra("com.krostiffer.krostrack.dist", mainAct.prefs!!.getFloat("DistBehind", -1.0f))
                                    intent.putExtra("com.krostiffer.krostrack.databaseid", dbid)
                                }
                        }
                    }else { //Wenn keine Route angeklickt wird, wird das starten unterbunden und eine Nachricht angezeigt
                        Toast.makeText(mainAct, getString(R.string.select_something_warning), Toast.LENGTH_LONG).show()
                        start = false
                    }
                }
                //wenn Modus 2, dann wird nur laufen an den Service ??bergeben
                2 -> {
                    Log.println(Log.ASSERT, "Button Press", "Mode 2")
                    runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                        intent.putExtra("com.krostiffer.krostrack.mode", "just run")
                    }
                }
                //Ein vierter Modus war geplant/k??nnte sp??ter, wenn ich Lust habe noch dazu kommen, in dem man gegen eine Durchschnittszeit bzw. eine Geschwindigkeit ??ber eine festgelegte Distanz l??uft
                //Dieser Code kann zurzeit nie erreicht werden
                3 -> {
                    Log.println(Log.ASSERT, "Button Press", "Mode 3")
                    runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                        intent.putExtra("com.krostiffer.krostrack.mode", "just run")
                    }
                }

            }

            if(start) { //Der Service wird gestartet, sofern Location an und wenn Modus 1 Route gew??hlt
                mainAct.run {
                    startService(runNotificationServiceIntent)
                    bindService(runNotificationServiceIntent, servconn, Context.BIND_AUTO_CREATE)
                }
                //K??nnte man smoother l??sen, aber der Startbutton verschwindet und der Stopbutton (an der selben position) erscheint
                stopButton.visibility = VISIBLE
                laufMaps.visibility = VISIBLE
                startButton.visibility = GONE
            }
        }



        //Stop wurde geklickt
        stopButton.setOnClickListener{
            Log.println(Log.ASSERT,"Stop Button", "Pressed")
            if(mainAct.ServiceisBound) {
                //K??nnte man smoother l??sen, aber der Stopbutton verschwindet und der Startbutton (an der selben position) erscheint
                stopButton.visibility = GONE
                laufMaps.visibility = GONE
                startButton.visibility = VISIBLE
                //Die LocationList des Services wird ??bernommen und der Service beendet und unbinded
                val locList = rnService.locList
                rnService.killService()
                mainAct.unbindService(servconn)
                mainAct.ServiceisBound = false

                if (locList.isNotEmpty() && locList.size > 1) { //sobald mindestens zwei Locations erfasst wurde, wird die Route in der Datenbank gespeichert
                    val l = addToDatabase(locList, locationDao)
                    val intent = Intent(mainAct, ShowMaps::class.java)
                    intent.putExtra("lat", l[0])
                    intent.putExtra("lon", l[1])
                    intent.putExtra("spe", l[2])
                    intent.putExtra("tim", l[3])
                    startActivity(intent)
                } else {
                    Toast.makeText(mainAct, getString(R.string.track_too_short_warning), Toast.LENGTH_LONG).show() //wenn weniger als zwei Positionen erfasst wurden, dann wird eine Nachricht angezeigt, dass die Strecke zu kurz war
                }

            } else {
                Log.println(Log.ASSERT,"Stop Button", "Service was NOT bound")
            }

        }

        return root
    }


    //Die gegebene Liste an Locations wird in die Datenbank in Form von Strings ??bertragen (Da keine komplexen Datentypen einfach unterst??tzt werden)
    //Die entsprechenden Strings werden wieder (als Liste) zur??ckgegeben
    private fun addToDatabase(locList: MutableList<Location>, locationDao: LocationDao): List<String>{
        var latitudes = ""
        var longitudes = ""
        var speeds = ""
        var times = ""
        //Die Werte werden mit # getrennt als String gespeichert
        for (loc in locList){
            latitudes = latitudes + "#" + loc.latitude.toString()
            longitudes = longitudes + "#" + loc.longitude.toString()
            speeds = speeds + "#" + loc.speed.toString()
            times = times + "#" + (loc.elapsedRealtimeNanos - locList.first().elapsedRealtimeNanos).toString()
        }

        locationDao.insertLocation(
            LocationExt(latitudes = latitudes, longitudes = longitudes, speeds = speeds, times = times, uid = 0,
            showTime = getDateTimeInstance().format(locList.first().time) //+ " - " +  getTimeInstance().format(locList.last().time)
        )
        )
        return listOf(latitudes, longitudes, speeds, times)
    }
    //Hilfsfunktion um einfach sharedPreferences in der MainActivity zu ??ndern
    fun changePref(side:String, value: Int) {
        val mainAct: MainActivity = activity as MainActivity
        val editor = mainAct.prefs!!.edit()
        editor.putInt(side, value)
        editor.apply()
    }

    //Stellt einige Einstellungen auf 0, wenn sie vorher nie gesetzt wurden (gab manchmal sehr komisches Verhalten, bevor ich diese Funktion eingef??gt habe)
    private fun prefResetIfNotSet() {
        val mainAct: MainActivity = activity as MainActivity
        val editor = mainAct.prefs!!.edit()
        var intVar = mainAct.prefs!!.getInt(mainAct.RIGHT_STORE, -2)
        if(intVar == -2)
            editor.putInt(mainAct.RIGHT_STORE, 0)
        intVar = mainAct.prefs!!.getInt(mainAct.MIDDLE_STORE, -2)
        if(intVar == -2)
            editor.putInt(mainAct.MIDDLE_STORE, 0)
        intVar = mainAct.prefs!!.getInt(mainAct.LEFT_STORE, -2)
        if(intVar == -2)
            editor.putInt(mainAct.LEFT_STORE, 0)
        intVar = mainAct.prefs!!.getInt(mainAct.MODE_STORE, -2)
        if(intVar == -2)
            editor.putInt(mainAct.MODE_STORE, 0)
        var floatVar: Float = mainAct.prefs!!.getFloat("TimeBehind", -2.0f)
        if(floatVar == -2.0f)
            editor.putFloat("TimeBehind", 5.0f)
        floatVar = mainAct.prefs!!.getFloat("DistBehind", -2.0f)
        if(floatVar == -2.0f)
            editor.putFloat("DistBehind", 6.0f)
        editor.apply()
    }

}