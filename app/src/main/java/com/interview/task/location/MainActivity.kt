package com.interview.task.location

import android.Manifest
import android.app.ProgressDialog
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.*


class MainActivity : AppCompatActivity() {

    //list of permissions
    private val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    //permission result code
    private val PERMISSIONS_RESULT_CODE: Int = 125

    //whether the user switched on the gps or not
    private var isGps: Boolean = false

    //location manager
    private lateinit var locationManager: LocationManager

    private lateinit var tvLocation: TextView

    //location client to connect to google play services
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    //location callback
    private lateinit var mLocationCallback: LocationCallback

    //progress Dialog
    private lateinit var mloadingDialog:ProgressDialog

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart: Button = findViewById(R.id.btnStart)
        val btnStop: Button = findViewById(R.id.btnStop)
        tvLocation = findViewById(R.id.tvLocation)
        mloadingDialog = ProgressDialog(this)
        mloadingDialog.setMessage("Fetching Location...")


        locationManager = getSystemService(Service.LOCATION_SERVICE) as LocationManager

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        btnStart.setOnClickListener {
            getLatLng()
        }

        btnStop.setOnClickListener {
            // locationManager.removeUpdates(this)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                if(mloadingDialog.isShowing){
                    mloadingDialog.dismiss()
                }
                for (obj in p0?.locations!!) {
                    tvLocation.text = "Latitude:-${obj.latitude},Longitude:-${obj.longitude},Time:-${obj.time}"

                }
            }
        }

    }

    /**
     * will return true if app has required permissions.
     */
    private fun hasRequiredPermissions(): Boolean = ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        application,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED


    /**
     * Will display the current loction to user in textview.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getLatLng() {
        if (hasRequiredPermissions()) {
            isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (isGps) {
                try {
                    mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            tvLocation.text =
                                    "Latitude:-${location.latitude},Longitude:-${location.longitude},Time:-${location.time}"
                        }
                        else {
                            tvLocation.text = ""
                            mloadingDialog.show()
                        }
                    }
                    setupPeriodicUpdates()
                } catch (e: SecurityException) {
                    checkAndRequestPermissions()
                }

            } else {
                Toast.makeText(this@MainActivity,"Please switch on location and gps services for the app to work",Toast.LENGTH_SHORT).show()
            }
        } else {
            checkAndRequestPermissions()
        }

    }


    /**
     * will setup periodic updates so that the location updates for every 5 seconds
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupPeriodicUpdates() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 5000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        val locationSettingsBuilder = LocationSettingsRequest.Builder()
        locationSettingsBuilder.addLocationRequest(mLocationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(locationSettingsBuilder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            try {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /* Looper */);
            } catch (e: SecurityException) {
                checkAndRequestPermissions()
            }
        }
    }

    /**
     * will check for permissions and will open permissions dialog
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "App has required permissions", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, perms, PERMISSIONS_RESULT_CODE)
        }
    }


    /**
     * if user rejects the permission than make a prompt and ask again
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_RESULT_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    getLatLng()
                } else {
                    requestPermissionsDialog()
                }
            }
        }
    }

    /**
     * request user as to why you need the permission
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermissionsDialog() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)

        alertDialogBuilder.setMessage("Please Allow Permissions for the app to work").setCancelable(false)
            .setPositiveButton(
                "OK"
            ) { p0, p1 ->
                checkAndRequestPermissions()
            }.show()
    }

}
