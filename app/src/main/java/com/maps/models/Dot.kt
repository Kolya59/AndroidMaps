package com.maps.models

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Dot : RealmObject() {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var path: String = ""

    fun getLatLng() = LatLng(lat, lng)

    fun setLatLng(latLng: LatLng) {
        lat = latLng.latitude
        lng = latLng.longitude
    }

    fun getURI() = Uri.parse(path)

    fun setURI(uri: Uri?) { path = uri?.path.toString() }
}