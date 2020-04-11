package com.maps.maps.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.maps.maps.R

class MapsActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener
{
    private lateinit var mMap: GoogleMap
    private var markerCounter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val perm = LatLng(58.008215, 56.1870133)
        // CameraUpdateFactory.zoomTo(15.0.toFloat())
        mMap.addMarker(MarkerOptions().position(perm).title("Marker in Perm"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(perm))

        // Bind listeners
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener(this)
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        val intent = Intent(this, MarkerActivity::class.java).apply {
            putExtra("com.maps.maps.NAME", p0?.title)
        }
        startActivity(intent)
        return true
    }

    override fun onMapClick(point: LatLng) {
        this.markerCounter++
        this.mMap.addMarker(MarkerOptions().position(point).title("Point ${this.markerCounter}"))
    }
}