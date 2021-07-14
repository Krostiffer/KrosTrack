package com.krostiffer.krostrack.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routeTable")
data class LocationExt(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "showTime") val showTime: String,
    @ColumnInfo(name = "latitudes") val latitudes: String,
    @ColumnInfo(name = "longitudes") val longitudes: String,
    @ColumnInfo(name = "times") val times: String,
    @ColumnInfo(name = "speeds") val speeds: String
)