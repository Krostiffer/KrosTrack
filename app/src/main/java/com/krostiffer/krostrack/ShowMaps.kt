package com.krostiffer.krostrack

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.style.StyleSpan
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.krostiffer.krostrack.databinding.ActivityShowMapsBinding
import kotlin.random.Random

class ShowMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityShowMapsBinding
    private lateinit var lat: List<String>
    private lateinit var lon: List<String>
    private lateinit var tim: List<String>
    private lateinit var spe: List<String>
    private var isNull: Boolean = true
    private var locList: MutableList<LatLng> = mutableListOf()
    private var timeList: MutableList<Long> = mutableListOf()
    private var speedList: MutableList<Double> = mutableListOf()

    //Übernimmt die gegebene Route und wandelt sie aus Strings in die entsprechenden Listen um
    override fun onCreate(savedInstanceState: Bundle?) {
        if(
            intent.getStringExtra("lat") != null &&
            intent.getStringExtra("lon") != null &&
            intent.getStringExtra("tim") != null &&
            intent.getStringExtra("spe") != null
        ) {
                lat = intent.getStringExtra("lat")!!.split("#")
                lon = intent.getStringExtra("lon")!!.split("#")
                tim = intent.getStringExtra("tim")!!.split("#")
                spe = intent.getStringExtra("spe")!!.split("#")
                for (i in 1 .. lat.lastIndex){
                    locList.add(LatLng(lat[i].toDouble(), lon[i].toDouble()))
                    timeList.add(tim[i].toLong())
                    speedList.add(spe[i].toDouble())
                }
                isNull = false
        } else {
            isNull = true
        }

        super.onCreate(savedInstanceState)

        binding = ActivityShowMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    //Zeigt eine einfarbige schwarze Linie als Route an
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //EINFARBIG
        mMap.addPolyline(PolylineOptions().addAll(locList))


        //for(pos in locList){
            //mMap.addMarker(MarkerOptions().position(pos))
        //    mMap.addPolyline(PolylineOptions().add(pos).color(Color.rgb(0,0, Random(1).nextInt())))
        //}

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locList[0], 15.0f))
    }
}