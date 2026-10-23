package com.example.data.db

import androidx.room.*
import com.example.data.model.Setlist
import kotlinx.coroutines.flow.Flow

@Dao
interface SetlistDao {
    @Query("SELECT * FROM setlists ORDER BY createdAt DESC")
    fun getAllSetlists(): Flow<List<Setlist>>

    @Query("SELECT * FROM setlists WHERE id = :id")
    suspend fun getSetlistById(id: Long): Setlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: Setlist): Long

    @Update
    suspend fun updateSetlist(setlist: Setlist)

    @Delete
    suspend fun deleteSetlist(setlist: Setlist)
}
