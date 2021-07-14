package com.krostiffer.krostrack.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationDao {
    @Query("SELECT * FROM routeTable")
    fun getAllRoutes(): List<LocationExt>

    @Query("SELECT * FROM routeTable WHERE uid = :id")
    fun getRoute(id: Int): LocationExt

    //@Insert
    //fun insertAll(vararg locationExt: com.krostiffer.krostrack.database.LocationExt)

    @Insert
    fun insertLocation(l: LocationExt)

    @Delete
    fun delete(locationExt: LocationExt)
}