package com.krostiffer.krostrack.ui.run

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeResource
import android.graphics.Color
import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.krostiffer.krostrack.MainActivity
import com.krostiffer.krostrack.R
import kotlin.reflect.typeOf

class RunMapsFragment : Fragment() {
    private var receiver: TmpReceiver? = null
    val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var mMap: GoogleMap
    lateinit var mapFragment: SupportMapFragment
    var mapReady: Boolean = false
    private var locList: MutableList<LatLng> = mutableListOf()
    private var recordedLocations: MutableList<LatLng> = mutableListOf()
    private var recordedTimes: MutableList<Long> = mutableListOf()

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        mapReady = true
        val sydney = LatLng(53.0, 53.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,18f))
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().bearing(2f).target(sydney).zoom(18f).build()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_run_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainAct: MainActivity = activity as MainActivity
        super.onViewCreated(view, savedInstanceState)
        mapFragment = (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(callback)
        receiver = TmpReceiver()
        val filter = IntentFilter().apply {
            addAction("LOCATION")
        }
        mainAct.registerReceiver(receiver, filter)

        mainHandler.post(object : Runnable {
            var once: Boolean = true
            var recordedindex = 0
            override fun run() {
                if(mapReady) {
                    //Log.println(Log.ASSERT, "Countdown", receiver!!.loc.first().toString())
                    val locationLatLng = LatLng(receiver!!.loc.first(), receiver!!.loc.last())
                    if (locationLatLng.latitude != 0.0 && locationLatLng.longitude != 0.0) {
                        locList.add(locationLatLng)

                        if (once) {
                            if (receiver!!.dbid >= 0) {
                                once = false
                                val dat =
                                    mainAct.locationDatabase.locationDao().getRoute(receiver!!.dbid)
                                val lat = dat.latitudes.split("#")
                                val lon = dat.longitudes.split("#")
                                val tim = dat.times.split("#")
                                for (i in 1..lat.lastIndex) {
                                    recordedLocations.add(LatLng(lat[i].toDouble(),
                                        lon[i].toDouble()))
                                    if(tim[1].toLong() == 0L)
                                        recordedTimes.add(tim[i].toLong())
                                    else
                                        recordedTimes.add(tim[i].toLong() - tim[1].toLong())
                                }
                                Log.println(Log.ASSERT, "Maps", recordedLocations.toString())
                                Log.println(Log.ASSERT, "Maps", recordedTimes.toString())
                            }
                        }
                        with(mMap) {
                            clear()
                            addPolyline(PolylineOptions().addAll(locList).color(Color.rgb(255,87,34)))
                            if (recordedLocations.size > 0) {
                                addPolyline(PolylineOptions().addAll(recordedLocations)
                                    .color(R.color.black))

                                for(i in recordedindex..recordedLocations.lastIndex){
                                    if(receiver!!.elapsedtime >= recordedTimes[recordedindex]){
                                        recordedindex = i
                                    }
                                }
                                Log.println(Log.ASSERT, "Maps: recordedIndex",
                                    recordedindex.toString() + " elapsedtime: " + receiver!!.elapsedtime.toString())
                                addMarker(MarkerOptions().position(recordedLocations[recordedindex])
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                            }
                            addMarker(MarkerOptions().position(locationLatLng)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                            moveCamera(CameraUpdateFactory.newLatLng(locationLatLng))
                        }
                    }
                }
                mainHandler.postDelayed(this, 1000)
            }
        })
    }

    class TmpReceiver: BroadcastReceiver() {
        var loc: DoubleArray = DoubleArray(2)
        var dbid = -1
        var elapsedtime: Long = -1
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1 != null) {
                loc = p1.getDoubleArrayExtra("location")!!
                dbid = p1.getIntExtra("databaseid", -1)
                elapsedtime = p1.getLongExtra("elapsedtime", -1)
                Log.println(Log.ASSERT,"Broadcast Receiver", "Received: $elapsedtime")

            } else {
                Log.println(Log.ASSERT,"Broadcast Receiver", "Nothing Received")
            }
        }
    }

}