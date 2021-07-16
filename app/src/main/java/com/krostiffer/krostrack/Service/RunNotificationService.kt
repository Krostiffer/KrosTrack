package com.krostiffer.krostrack.Service

import android.Manifest
import android.app.*
import android.app.Notification.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.math.MathUtils.lerp
import com.krostiffer.krostrack.R
import kotlin.math.abs
import com.google.android.material.math.MathUtils

class RunNotificationService : Service() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notification: Notification
    val UNDEFINED_COORD = 6000.0 //random hoher Wert, der nicht als Latitude oder Longitude auftaucht (alles über 90 sollte ausreichen)
    var locList: MutableList<Location> = mutableListOf()
    var timeList = mutableListOf<Long>()
    private var speedList = mutableListOf<Double>()
    private var recordedLocationList = mutableListOf<LatLng>()
    var distanceList = mutableListOf<Double>(0.0)
    private var wantedSpeed: Double = 0.0
    private var startId:Int = 0
    private var notID = 745
    private val rnsBinder = RnsLocalBinder()
    private var left = 0
    private var middle = 0
    private var right = 0
    var mode = ""
    var currentDistance = 0.0
    var currentIndex = 0
    var lastLocation: DoubleArray = DoubleArray(2)
    var startTime: Long = 0
    var lastGoodSpeedNanoseconds: Long = 0
    var nanosecondsUnderSetSpeed: Long = 0
    var MAX_TIME_UNDER_SET_SPEED = 5000000000

    override fun onBind(intent: Intent): IBinder {
        return rnsBinder
    }

    inner class RnsLocalBinder : Binder() {
        fun getService() : RunNotificationService {
            return this@RunNotificationService
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            mode = intent.getStringExtra("com.krostiffer.krostrack.mode")!!
            Log.println(Log.ASSERT, "Service Mode", mode)
            when (mode) {
                "just speed" -> { //wenn gegen feste Geschwindigkeit, dann werden die SliderWerte übernommen
                    left = intent.getIntExtra("com.krostiffer.krostrack.left", -2)
                    middle = intent.getIntExtra("com.krostiffer.krostrack.middle", -2)
                    right = intent.getIntExtra("com.krostiffer.krostrack.right", -2)
                    if (left < 0 || middle < 0 || right < 0) {
                        Log.println(Log.ASSERT, "Service Error", "values are not correct")
                    }
                }
                "just run" -> {

                }

                "run vs self" -> { //Wenn gegen eine Aufzeichnung gelaufen wird, dann wird die entsprechende Aufzeichnung geladen

                    val nSpeeds = intent.getStringExtra("com.krostiffer.krostrack.speeds")
                    val nTimes = intent.getStringExtra("com.krostiffer.krostrack.times")
                    val nLat = intent.getStringExtra("com.krostiffer.krostrack.latitudes")
                    val nLon = intent.getStringExtra("com.krostiffer.krostrack.longitudes")

                    if (nSpeeds == null || nTimes == null || nLat == null || nLon == null) {
                        Log.println(Log.ASSERT, "Service Error", "values are not correct")
                        Toast.makeText(this, getString(R.string.something_went_wrong_warning), Toast.LENGTH_LONG).show()
                    } else {
                        val lat = nLat.split("#")
                        val lon = nLon.split("#")
                        val tim = nTimes.split("#")
                        val spe = nSpeeds.split("#")

                        //for geht von 1 bis vorletztes Element, da Strings vom Aufbau "#one#duo#drei" und durch split der erste eintrag ein leerer String ist. Bis zum vorletzten, da immer der aktuelle und nachfolgende Punkt betrachtet wird
                        for (i in 1 until lat.lastIndex){ //gespeicherte Strings müssen in passende Listen konvertiert Werden
                            recordedLocationList.add(LatLng(lat[i].toDouble(), lon[i].toDouble())) //Momentan überflüssig, Koversion von Latitude und Longitude String in LatLng Liste
                            timeList.add((tim[i].toLong() - tim[1].toLong())) //Speichern der Zeit als Liste der Zeit nach Beginn der Aufzeichnung
                            speedList.add(spe[i].toDouble()) //Momentan überflüssig, Speichern der Geschwindigkeiten als Liste

                            val distArray = FloatArray(1) //hilfsarray für abstandsmessung
                            Location.distanceBetween(lat[i].toDouble(),  lon[i].toDouble(), lat[i+1].toDouble(),  lon[i+1].toDouble(), distArray) //Abstandsmessung zwischen aktuellem und nächsten gespeicherten Punkt
                            distanceList.add(distanceList.last() + distArray[0].toDouble()) //Speicherung der Abstände zwischen Punkten als Liste der Abstände vom Start
                        }
                        Log.println(Log.ASSERT, "Service List Test", timeList.toString())
                        Log.println(Log.ASSERT, "Service List Test", distanceList.toString())

                    }
                }

            }

        }

        this.startId = startId
        Log.println(Log.ASSERT, "Service", "Service started")

        notficationUpdate("", false)

        startForeground(
            notID,
            notification
        )


        return super.onStartCommand(intent, flags, startId)
    }

    fun returnLocationList(): MutableList<Location> {
        return locList
    }

    fun killService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        mFusedLocationClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        lastLocation = doubleArrayOf(UNDEFINED_COORD, UNDEFINED_COORD) //letzte Location auf undefined setzen
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    calculateWantedSpeed()
                    locList.add(location)
                    if(startTime.toInt() == 0)
                        startTime = location.elapsedRealtimeNanos
                    val dArray = FloatArray(1)
                    Location.distanceBetween(lastLocation[0],  lastLocation[1], location.latitude,  location.longitude, dArray)
                    if(lastLocation[0] != UNDEFINED_COORD && lastLocation[1] != UNDEFINED_COORD) //distanz wird erst ab der zweiten location erhöht (da aus einer position keine distanz ermittelt werden kann)
                        currentDistance += dArray[0]
                    lastLocation = doubleArrayOf(location.latitude, location.longitude)
                    when (mode) {
                        "just speed" -> { //Es wird die letzte "gute" (>= angegeben) Geschwindigkeit bestimmt; Wenn nach einer Zeit (momentan fest 5s) keine "gute" Geschwindigkeit erreicht wird, dann fängt das Handy an zu vibrieren
                            if (locList.size == 1) {
                                lastGoodSpeedNanoseconds = location.elapsedRealtimeNanos
                            }
                            if (location.speed < wantedSpeed) {
                                nanosecondsUnderSetSpeed =
                                    location.elapsedRealtimeNanos - lastGoodSpeedNanoseconds

                            } else {
                                lastGoodSpeedNanoseconds = location.elapsedRealtimeNanos
                                nanosecondsUnderSetSpeed = 0
                            }
                            if (nanosecondsUnderSetSpeed > MAX_TIME_UNDER_SET_SPEED) {
                                timeVibrationPattern()
                            }
                            notficationUpdate((location.speed * 3.6).toString() + "km/h  [" + wantedSpeed + " km/h]", true)
                        }
                        "run vs self" -> {
                            //berechnet die Strecke, die aktuelle zurückgelegt wurde und vergleicht sie mit der Strecke, die zum aktuellen Zeitpunkt in der Aufzeichnung zurückgelegt wurde
                            val t = location.elapsedRealtimeNanos - startTime //aktuelle Zeit in der Route (Zeit nach Start der Aufzeichnung)
                            var over = false
                            while (timeList[currentIndex+1] < t) { //index so weit hochstellen bis t zwischen aktuellem und nächsthöhrerer Zeit liegt
                                currentIndex++
                                if (currentIndex+1 > timeList.size){ //sonderfall: Aufzeichnung ist vorbei
                                    currentIndex = timeList.size
                                    over = true
                                    break
                                }
                            }
                            val maxBehind = 6 //hardcodierte Meter hinter der Aufzeichnung
                            if(!over){ //normalfall, die Aufzeichnung ist noch nicht vorbei

                                Log.println(Log.ASSERT,"TimeListCur", "$currentIndex , ${timeList[currentIndex] / 1000000000} < [${t/ 1000000000}] < ${timeList[currentIndex + 1] / 1000000000}  ")
                                val amount: Double = (t - timeList[currentIndex].toDouble())/(timeList[currentIndex+1] - timeList[currentIndex]).toDouble()
                                val calcDistance = lerp(distanceList[currentIndex].toFloat(), distanceList[currentIndex + 1].toFloat(), amount.toFloat())
                                Log.println(Log.ASSERT,"Calc Distance/amount/curDist", "$amount: ${distanceList[currentIndex]} < $calcDistance < ${distanceList[currentIndex+1]}// $currentDistance")
                                var d = calcDistance - currentDistance
                                val prefix: String = if(d < 0){
                                    getString(R.string.ahead)
                                } else {
                                    getString(R.string.behind)

                                }

                                if(d > maxBehind) {
                                    distanceVibrationPattern(abs(d))
                                }
                                d = abs(d)
                                notficationUpdate("$d $prefix", true)
                            } else {
                                var d = distanceList.last() - currentDistance
                                val prefix: String = if(d < 0){
                                    getString(R.string.ahead)
                                } else {
                                    getString(R.string.behind)
                                }
                                if(d > maxBehind) {
                                    distanceVibrationPattern(abs(d))
                                }
                                d = abs(d)
                                notficationUpdate("$d $prefix ${getString(R.string.alreadyFinished)}", true)
                            }



                        }
                        "just run" -> { // aktualisiert einfach die Nachricht mit der aktuellen Geschwindigkeit
                            notficationUpdate((location.speed * 3.6).toString() + "km/h", true)
                        }
                    }


                    //Log.println(Log.ASSERT,"Notification", location.toString())
                    //fulltext = location.speed.toString() + "\n" + location.bearing
                }
            }
        }
        startLocationUpdates()


    }
    private fun timeVibrationPattern() { //Vibrations"muster" für laufen gegen fixe geschwindigkeit, momentan einfach linear skalierend mit der Zeit, die unter der angegebenen Geschwindigkeit verbracht wurde
        val vib =  getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        //var scledSeconds = (nanosecondsUnderSetSpeed - MAX_TIME_UNDER_SET_SPEED) / 1000000000

        //var timings : LongArray = longArrayOf(scledSeconds * 10, scledSeconds * 30, scledSeconds * 50)

        val vibEffect: VibrationEffect = VibrationEffect.createOneShot(nanosecondsUnderSetSpeed / 30000000, (100 - (3/(nanosecondsUnderSetSpeed / 1000000000))).toInt())
        vib.vibrate(vibEffect)

    }
    private fun distanceVibrationPattern(d: Double) {
        val vib =  getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibEffect: VibrationEffect = VibrationEffect.createOneShot((d * 35).toLong(), 100)
        vib.vibrate(vibEffect)

    }
    private fun calculateWantedSpeed() {
        when (mode) {
            "just speed" -> {
                if(right == 0){
                    //Log.println(Log.ASSERT,"speed rechnung", "kmh")
                    wantedSpeed = (left + (middle * 0.01)) / 3.6
                    //Log.println(Log.ASSERT,"speed rechnung", wantedSpeed.toString())
                }
                if(right == 1){
                    //Log.println(Log.ASSERT,"speed rechnung", "pace")
                    wantedSpeed = 1000/(left * 60.0 + middle)
                    //Log.println(Log.ASSERT,"speed rechnung: Sekunden", ((left * 60.0 + middle)).toString())
                }
            }
            "just run" -> {
                wantedSpeed = 0.0
            }
        }
    }
    private fun notficationUpdate(content: String, notify: Boolean): Notification {

        val pendingIntent: PendingIntent =
            Intent(this, RunNotificationService::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel("ForegroundID", getText(R.string.notification_setting_name_background_tracking), NotificationManager.IMPORTANCE_LOW))

        notification = Builder(this, "ForegroundID")
            .setContentTitle(getText(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(R.drawable.run)
            .setContentIntent(pendingIntent)
            .build()

        if(notify) {
            notificationManager.notify(notID, notification)
        }

        return notification
    }

    private fun startLocationUpdates() {

        Log.println(Log.ASSERT, "Service", "Location Updates started")
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, getString(R.string.permissionNotGranted), Toast.LENGTH_LONG).show()
            Log.println(Log.ASSERT, "Service", "locUpdateERROR")
            return
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            Log.println(Log.ASSERT, "Service", "locUpdateERROR2")
        mFusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
        Log.println(Log.ASSERT, "Service", "locUpdate2")
    }


}