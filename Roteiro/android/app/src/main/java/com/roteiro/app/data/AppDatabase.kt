package com.roteiro.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places")
    suspend fun getAll(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun get(id: Long): PlaceEntity?

    @Insert
    suspend fun insert(place: PlaceEntity): Long

    @Update
    suspend fun update(place: PlaceEntity)

    @Delete
    suspend fun delete(place: PlaceEntity)

    @Query("DELETE FROM places")
    suspend fun deleteAll()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE archived = 0 OR done = 1 ORDER BY createdAt")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE placeId = :placeId")
    suspend fun getByPlace(placeId: Long): List<ItemEntity>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Update
    suspend fun updateAll(items: List<ItemEntity>)

    @Query("UPDATE items SET placeId = NULL, remind = CASE WHEN remind IN ('ARRIVE', 'LEAVE') THEN 'NONE' ELSE remind END WHERE placeId = :placeId")
    suspend fun detachFromPlace(placeId: Long)

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}

@Database(entities = [PlaceEntity::class, ItemEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun places(): PlaceDao
    abstract fun items(): ItemDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "roteiro.db").build()
    }
}
