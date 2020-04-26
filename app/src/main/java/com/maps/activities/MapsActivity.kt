package com.maps.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.maps.models.Dot
import com.maps.store.Store

class MapsActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {
    private lateinit var mMap: GoogleMap
    private lateinit var mStore: Store
    private lateinit var mLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mStore = Store(this)

        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapClickListener(this)
        mMap.setOnMarkerClickListener(this)

        mStore.findAllMarkers()?.forEach { mMap.addMarker(MarkerOptions().position(it.getLatLng())) }

        if (!requestStoragePermissions()) {
            Log.e(getString(R.string.log_tag), "Required storage permissions not granted")
            finish()
        }

        if (!requestLocationPermissions()) {
            Log.e(getString(R.string.log_tag), "Required location permissions not granted")
            finish()
        }

        mLocationClient = LocationServices.getFusedLocationProviderClient(this)
        subscribeForLocationChanging()
    }

    override fun onMapClick(position: LatLng?) {
        position?.let { mStore.createMarker(it) }
        mMap.addMarker(position?.let { MarkerOptions().position(it) })
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        if (marker != null) {
            val intent = Intent(this, MarkerActivity::class.java)
            intent.putExtra("key", marker.position)
            startActivity(intent)
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isLocationEnabled()) {
            mMap.isMyLocationEnabled = true
            subscribeForLocationChanging()
        }
    }

    override fun onDestroy() {
        this.mStore.close()
        super.onDestroy()
    }

    private fun requestStoragePermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(
            this,
            permissions,
            0
        )
        return checkPermissions(permissions)
    }

    private fun requestLocationPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        ActivityCompat.requestPermissions(
            this,
            permissions,
            1
        )
        return checkPermissions(permissions)
    }

    private fun checkPermissions(permissions: Array<String>): Boolean =
        permissions.fold(true) { acc, p ->
            acc && ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
        }

    private fun subscribeForLocationChanging() {
        if (requestLocationPermissions()) {
            if (isLocationEnabled()) {
                mMap.isMyLocationEnabled = true
                listenLocationChanging()
            } else {
                notify("Location disabled")
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    private fun listenLocationChanging() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 15 * 1000

        val mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val nearest = mutableListOf<Dot>()
                mStore.findAllMarkers()?.forEach { dot ->
                    val target = Location("")
                    target.latitude = dot.getLatLng().latitude
                    target.longitude = dot.getLatLng().longitude
                    if (locationResult.lastLocation.distanceTo(target) <= 1000) {
                        nearest.add(dot)
                    }
                }
                if (nearest.size != 0) notify("These markers are close with you: $nearest")
            }
        }

        mLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun notify(msg: String) {
        val toast = Toast.makeText(this, msg, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
    }
}