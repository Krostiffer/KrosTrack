package com.krostiffer.krostrack.ui.run

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.krostiffer.krostrack.*
import com.krostiffer.krostrack.Service.RunNotificationService
import com.krostiffer.krostrack.database.LocationDao
import com.krostiffer.krostrack.database.LocationDatabase
import com.krostiffer.krostrack.database.LocationExt
import com.krostiffer.krostrack.databinding.FragmentVsYourselfBinding
import com.krostiffer.krostrack.ui.run.modes.JustRunFragment
import com.krostiffer.krostrack.ui.run.modes.SpeedSetting
import com.krostiffer.krostrack.ui.run.modes.VsYourselfFragment
import java.text.DateFormat.getDateTimeInstance
import java.text.DateFormat.getTimeInstance


class RunFragment : Fragment() {

    lateinit var runNotificationServiceIntent: Intent
    lateinit var rnService: RunNotificationService
    lateinit var servconn: ServiceConnection
    var isBound = false
    val nrOfMenus = 3
    val COARSE_REQUEST_CODE = 100
    val FINE_REQUEST_CODE = 101
    val BACKGROUND_REQUEST_CODE = 102

    private inner class PagerAdapter(f:FragmentActivity) : FragmentStateAdapter(f) {
        var flist = listOf(SpeedSetting(), VsYourselfFragment(), JustRunFragment()) //Nach Zeit, Nach Route, Einfach so

        override fun getItemCount(): Int {
            return nrOfMenus
        }

        override fun createFragment(position: Int): Fragment {
            return flist[position]
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkPrefs()
        val root = inflater.inflate(R.layout.fragment_run, container, false)
        val mainAct: MainActivity = activity as MainActivity
        val tabLayout = root.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = root.findViewById<ViewPager2>(R.id.viewPager)
        val locationDatabase: LocationDatabase = mainAct.locationDatabase
        val locationDao: LocationDao = locationDatabase.locationDao()
        viewPager.adapter = PagerAdapter(f = mainAct)
        viewPager.currentItem = mainAct.prefs!!.getInt(mainAct.MODE_STORE, 0)
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
        var vsModeChange = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                changePref(mainAct.MODE_STORE, position)
            }
        }
        viewPager.registerOnPageChangeCallback(vsModeChange)


        servconn = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName,
                                            service: IBinder
            ) {
                val binder = service as RunNotificationService.RnsLocalBinder
                rnService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.println(Log.ASSERT,"ServiceConnection", "Service Disconnected")
                isBound = false
            }
        }
        if( ActivityCompat.checkSelfPermission(mainAct, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
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
        stopButton.visibility = GONE
        startButton.visibility = VISIBLE

        startButton.setOnClickListener{
            Log.println(Log.ASSERT, "Button Press", "Service starting...")
            var start = true
            when(mainAct.prefs!!.getInt(mainAct.MODE_STORE, -1)) {
                -1 -> {
                    Log.println(Log.ASSERT, "Button Press", "Error")
                }
                0 -> {
                    runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                        intent.putExtra("com.krostiffer.krostrack.mode", "just speed")
                        intent.putExtra("com.krostiffer.krostrack.left", mainAct.prefs!!.getInt(mainAct.LEFT_STORE, -1))
                        intent.putExtra("com.krostiffer.krostrack.middle", mainAct.prefs!!.getInt(mainAct.MIDDLE_STORE, -1))
                        intent.putExtra("com.krostiffer.krostrack.right", mainAct.prefs!!.getInt(mainAct.RIGHT_STORE, -1))
                    }
                }
                1 -> {
                    Log.println(Log.ASSERT, "Button Press", "Mode 1")
                    var dbid = mainAct.prefs!!.getInt(mainAct.SELECTED_UID_FROM_DATABASE, -1)
                    if(dbid > 0) {
                        var route = mainAct.locationDatabase.locationDao().getRoute(dbid)
                        if(route != null && route.latitudes.isNotEmpty()) {
                            runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                                    intent.putExtra("com.krostiffer.krostrack.mode", "run vs self")
                                    intent.putExtra("com.krostiffer.krostrack.speeds", route.speeds)
                                    intent.putExtra("com.krostiffer.krostrack.times", route.times)
                                    intent.putExtra("com.krostiffer.krostrack.latitudes", route.latitudes)
                                    intent.putExtra("com.krostiffer.krostrack.longitudes", route.longitudes)
                                }
                        }
                    }else {
                        Toast.makeText(mainAct, getString(R.string.select_something_warning), Toast.LENGTH_LONG).show()
                        start = false
                    }
                }
                2 -> {
                    Log.println(Log.ASSERT, "Button Press", "Mode 2")
                    runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                        intent.putExtra("com.krostiffer.krostrack.mode", "just run")
                    }
                }
                3 -> {
                    Log.println(Log.ASSERT, "Button Press", "Mode 3")
                    runNotificationServiceIntent = Intent(mainAct, RunNotificationService::class.java).also { intent ->
                        intent.putExtra("com.krostiffer.krostrack.mode", "just run")
                    }
                }

            }

            if(start) {
                mainAct.run {
                    startService(runNotificationServiceIntent)
                    bindService(runNotificationServiceIntent, servconn, Context.BIND_AUTO_CREATE)
                }
                stopButton.visibility = VISIBLE
                startButton.visibility = GONE
            }
        }



        stopButton.setOnClickListener{
            Log.println(Log.ASSERT,"Stop Button", "Pressed")
            if(isBound) {
                stopButton.visibility = GONE
                startButton.visibility = VISIBLE
                var locList = rnService.locList
                rnService.killService()
                mainAct.unbindService(servconn)
                isBound = false
                if (locList.isNotEmpty()) {
                    var l = addToDatabase(locList, locationDao)
                    var intent = Intent(mainAct, ShowMaps::class.java)
                    intent.putExtra("lat", l[0])
                    intent.putExtra("lon", l[1])
                    intent.putExtra("spe", l[2])
                    intent.putExtra("tim", l[3])
                    startActivity(intent)
                } else {
                    Toast.makeText(mainAct, getString(R.string.track_too_short_warning), Toast.LENGTH_LONG).show()
                }

            } else {
                Log.println(Log.ASSERT,"Stop Button", "Service was NOT bound")
            }

        }

        return root
    }


    fun addToDatabase(locList: MutableList<Location>, locationDao: LocationDao): List<String>{
        var latitudes: String = ""
        var longitudes: String = ""
        var speeds: String = ""
        var times: String = ""

        for (loc in locList){
            latitudes = latitudes + "#" + loc.latitude.toString()
            longitudes = longitudes + "#" + loc.longitude.toString()
            speeds = speeds + "#" + loc.speed.toString()
            times = times + "#" + loc.elapsedRealtimeNanos.toString()
        }
        Log.println(Log.ASSERT,"Database in RunFragment", latitudes)
        locationDao.insertLocation(
            LocationExt(latitudes = latitudes, longitudes = longitudes, speeds = speeds, times = times, uid = 0,
            showTime = getDateTimeInstance().format(locList.first().time) + " - " +  getTimeInstance().format(locList.last().time)
        )
        )
        return listOf(latitudes, longitudes, speeds, times)
    }

    fun changePref(side:String, value: Int) {
        val mainAct: MainActivity = activity as MainActivity
        val editor = mainAct.prefs!!.edit()
        editor.putInt(side, value)
        editor.apply()
    }

    fun checkPrefs() {
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
        editor.apply()
    }

}