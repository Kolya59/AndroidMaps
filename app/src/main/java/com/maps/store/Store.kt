package com.maps.store

import android.content.Context
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.maps.models.Dot
import io.realm.Realm
import io.realm.RealmResults

class Store(context: Context) {
    private var mRealm: Realm

    init {
        Realm.init(context)
        mRealm = Realm.getDefaultInstance()
    }

    fun findAllMarkers(): RealmResults<Dot>? = if (checkConnectionOpening()) {
            mRealm.where(Dot::class.java).findAll()
        } else null


    fun findMarker(position: LatLng?): Dot? = if (checkConnectionOpening() && position != null) {
            mRealm.where(Dot::class.java)
                .equalTo("lat", position.latitude)
                .equalTo("lng", position.longitude)
                .findFirst()
        } else null

    fun createMarker(position: LatLng) {
        if (!checkConnectionOpening()) {return}
        mRealm.executeTransaction {
            val dot = Dot()
            dot.setLatLng(position)
            this.mRealm.copyToRealm(dot)
        }
    }

    fun updateMarkerUri(dot: Dot, uri: Uri) {
        if (!checkConnectionOpening()) {return}
        this.mRealm.executeTransaction {
            val updated = mRealm.where(Dot::class.java)
                .equalTo("lat", dot.getLatLng().latitude)
                .equalTo("lng", dot.getLatLng().longitude)
                .findFirst()
            updated!!.setURI(uri)
            mRealm.copyToRealmOrUpdate(updated)
        }
    }

    private fun checkConnectionOpening() = !this.mRealm.isClosed

    fun close() { if (checkConnectionOpening()) { mRealm.close() } }
}