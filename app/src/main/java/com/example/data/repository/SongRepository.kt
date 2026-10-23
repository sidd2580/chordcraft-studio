package com.example.data.repository

import com.example.data.db.SetlistDao
import com.example.data.db.SongDao
import com.example.data.model.Setlist
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SongRepository(
    private val songDao: SongDao,
    private val setlistDao: SetlistDao
) {

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val allSetlists: Flow<List<Setlist>> = setlistDao.getAllSetlists()

    suspend fun initializeDefaultsIfEmpty() {
        val existing = songDao.getAllSongs().first()
        if (existing.isEmpty()) {
            val initialSongs = SampleSongCatalogue.getDefaultSongs()
            val ids = songDao.insertSongs(initialSongs)

            // Seed initial sample Setlists
            val gigSongIds = ids.take(4)
            val rehearsalSongIds = ids.drop(2).take(3)

            setlistDao.insertSetlist(
                Setlist(
                    name = "Friday Night Acoustic Gig",
                    description = "Main set at Blue Note Lounge - 45 min",
                    category = "Gigs",
                    songIds = gigSongIds,
                    performanceNotes = "Use Capo 2 on Wonderwall. Keep tempo steady on Hotel California."
                )
            )

            setlistDao.insertSetlist(
                Setlist(
                    name = "Band Rehearsal Warmup",
                    description = "Studio rehearsal rhythm section",
                    category = "Rehearsals",
                    songIds = rehearsalSongIds,
                    performanceNotes = "Check vocal harmonies on Let It Be."
                )
            )
        }
    }

    fun getSongsByCategory(category: String): Flow<List<Song>> {
        return songDao.getSongsByCategory(category)
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query)
    }

    suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)
    }

    suspend fun getSongsByIds(ids: List<Long>): List<Song> {
        return songDao.getSongsByIds(ids)
    }

    suspend fun saveSong(song: Song): Long {
        return if (song.id == 0L) {
            songDao.insertSong(song)
        } else {
            songDao.updateSong(song)
            song.id
        }
    }

    suspend fun deleteSong(song: Song) {
        songDao.deleteSong(song)
    }

    suspend fun toggleFavorite(song: Song) {
        songDao.updateSong(song.copy(isFavorite = !song.isFavorite))
    }

    suspend fun saveSetlist(setlist: Setlist): Long {
        return if (setlist.id == 0L) {
            setlistDao.insertSetlist(setlist)
        } else {
            setlistDao.updateSetlist(setlist)
            setlist.id
        }
    }

    suspend fun deleteSetlist(setlist: Setlist) {
        setlistDao.deleteSetlist(setlist)
    }
}
