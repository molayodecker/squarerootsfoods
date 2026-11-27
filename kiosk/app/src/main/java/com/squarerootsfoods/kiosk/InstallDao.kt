package com.squarerootsfoods.kiosk

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface InstallDao {
    @Query("SELECT * FROM install_visits ORDER BY id DESC LIMIT 1")
    suspend fun latestVisit(): InstallVisit?

    @Query("SELECT * FROM install_visits WHERE id = :id")
    suspend fun visit(id: Long): InstallVisit?

    @Insert
    suspend fun insertVisit(visit: InstallVisit): Long

    @Update
    suspend fun updateVisit(visit: InstallVisit)

    @Query("DELETE FROM lane_marks WHERE visitId = :visitId")
    suspend fun clearMarks(visitId: Long)

    @Insert
    suspend fun insertMarks(marks: List<LaneMark>)

    @Query("SELECT * FROM lane_marks WHERE visitId = :visitId ORDER BY lane")
    suspend fun marks(visitId: Long): List<LaneMark>

    @Insert
    suspend fun insertEvent(event: InstallEvent)
}
