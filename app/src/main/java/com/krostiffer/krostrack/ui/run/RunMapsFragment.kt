package com.krostiffer.krostrack.ui.run

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeResource
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

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        mapReady = true
        val sydney = LatLng(53.0, 53.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,18f))
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
            override fun run() {
                if(mapReady) {
                    //Log.println(Log.ASSERT, "Countdown", receiver!!.loc.first().toString())
                    val locationLatLng = LatLng(receiver!!.loc.first(), receiver!!.loc.last())
                    if (locationLatLng.latitude != 0.0 && locationLatLng.longitude != 0.0) {
                        locList.add(locationLatLng)
                    }

                    if(once){
                        if(receiver!!.dbid >=0){
                            once = false
                            val dat = mainAct.locationDatabase.locationDao().getRoute(receiver!!.dbid)
                            val lat = dat.latitudes.split("#")
                            val lon = dat.longitudes.split("#")
                            for (i in 1 .. lat.lastIndex) {
                                recordedLocations.add(LatLng(lat[i].toDouble(), lon[i].toDouble()))
                            }
                            Log.println(Log.ASSERT,"Maps", recordedLocations.toString())
                        }
                    }
                    with(mMap) {
                        clear()
                        addPolyline(PolylineOptions().addAll(locList).color(R.color.orange_500))
                        if(recordedLocations.size > 0)
                            addPolyline(PolylineOptions().addAll(recordedLocations).color(R.color.black))
                        addMarker(MarkerOptions().position(locationLatLng)
                                .title("Current Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                        moveCamera(CameraUpdateFactory.newLatLng(locationLatLng))
                    }
                }
                mainHandler.postDelayed(this, 1000)
            }
        })
    }

    class TmpReceiver: BroadcastReceiver() {
        var loc: DoubleArray = DoubleArray(2)
        var dbid = -1
        private var locList: MutableList<LatLng> = mutableListOf()
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1 != null) {
                Log.println(Log.ASSERT,"Broadcast Receiver", p1.action + " Received")
                loc = p1.getDoubleArrayExtra("location")!!
                dbid = p1.getIntExtra("databaseid", -1)
            } else {
                Log.println(Log.ASSERT,"Broadcast Receiver", "Nothing Received")
            }
        }
    }

}