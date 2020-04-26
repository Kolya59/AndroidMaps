package com.maps.store

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.maps.models.Dot
import io.realm.Realm
import io.realm.RealmResults
import java.util.*

class Store(context: Context) {
    private lateinit var realm: Realm
    private val logTag = "store"

    init {
        Realm.init(context)
        this.realm = Realm.getDefaultInstance()
    }

    // Закрытие соединения
    fun close() {
        if (checkConnectionOpening()) {
            this.realm.close()
        }
    }

    // Запрос всех маркеров
    fun findAllMarkers(): RealmResults<Dot>? {
        if (!checkConnectionOpening()) { return null }
        return try {
            this.realm.where(Dot::class.java).findAll()
        } catch (e: Exception) {
            Log.e(logTag, "Find markers failed: ${e.message}")
            null
        }
    }

    // Создание маркера
    fun createMarker(position: LatLng) {
        if (!checkConnectionOpening()) {return}
        try {
            this.realm.executeTransaction {
                val dot = Dot()
                dot.setLatLng(position)
                this.realm.copyToRealm(dot)
            }
        }
        catch(e: Exception) {
            Log.e(logTag, "Creating marker failed: ${e.message}")
        }
    }

    // Обновление фото маркера
    fun updateMarkerImage(position: LatLng, uri: Uri?) {
        if (!checkConnectionOpening()) {return}
        try {
            this.realm.executeTransaction {
                val dot = this.realm.where(Dot::class.java)
                    .equalTo("lat", position.latitude)
                    .equalTo("lng", position.longitude)
                    .findFirst()
                if (dot == null) {
                    Log.e(logTag, "Marker not found")
                    return@executeTransaction
                }
                if (uri != null) {
                    dot.setURI(uri)
                }
            }
        }
        catch(ex :Exception) {
            Log.e(logTag, "Updating marker error: ${ex.message}")
        }
    }

    // Проверка открытия соединения
    private fun checkConnectionOpening(): Boolean {
        return !this.realm.isClosed
    }
}