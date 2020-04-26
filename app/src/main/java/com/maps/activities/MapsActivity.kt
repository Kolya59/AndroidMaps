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
import com.google.android.gms.maps.model.MarkerOptions
import com.maps.models.Dot
import com.maps.store.Store

class MapsActivity :
    AppCompatActivity(),
    OnMapReadyCallback
{
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

        mStore.findAllMarkers()?.forEach { mMap.addMarker(MarkerOptions().position(it.getLatLng())) }

        if (!requestStoragePermissions() ||
            !requestLocationPermissions()
        ) {
            Log.e(getString(R.string.log_tag), "Required permissions not granted")
            finish()
        }

        mLocationClient = LocationServices.getFusedLocationProviderClient(this)
        subscribeForLocationChanging()

        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(58.008215, 56.1870133)))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == resources.getInteger(R.integer.permission_location_id)) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                subscribeForLocationChanging()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == resources.getInteger(R.integer.location_req_code) && isLocationEnabled()) {
            mMap.isMyLocationEnabled = true
            subscribeForLocationChanging()
        }
    }

    override fun onDestroy() {
        this.mStore.close()
        super.onDestroy()
    }

    // Запрос разрешений
    private fun requestStoragePermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(
            this,
            permissions,
            resources.getInteger(R.integer.permission_storage_id)
        )

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

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
            resources.getInteger(R.integer.permission_location_id)
        )
        return checkPermissions(permissions)
    }

    private fun checkPermissions(permissions: Array<String>): Boolean = permissions.fold(true) { acc, p ->
        acc && ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    }

    private fun subscribeForLocationChanging() {
        if (requestLocationPermissions()) {
            if (isLocationEnabled()) {
                mMap.isMyLocationEnabled = true
                listenLocationChanging()
            } else {
                notify("Location disabled")
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, resources.getInteger(R.integer.location_req_code))
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
                    if (locationResult.lastLocation.distanceTo(target) <=
                        resources.getInteger(R.integer.ping_range)) {
                        nearest.add(dot)
                    }
                }
                notify("These markers are close with you: $nearest")
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
        val toast = Toast.makeText(this,  msg, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
    }
}