package com.roteiro.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    /** Para os lugares "Qualquer …": se o tipo já existe, não insere de novo. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(place: PlaceEntity): Long

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

@Database(entities = [PlaceEntity::class, ItemEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun places(): PlaceDao
    abstract fun items(): ItemDao

    companion object {
        /** v2: lugares do tipo "qualquer mercado" (coluna category). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE places ADD COLUMN category TEXT")
            }
        }

        /**
         * v3: os lugares "Qualquer …" chegaram a ser criados em dobro (duas inicializações ao mesmo tempo).
         * Junta os repetidos no mais antigo, levando os itens junto, e impede novas cópias.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """UPDATE items SET placeId = (
                        SELECT MIN(p2.id) FROM places p2
                        WHERE p2.category = (SELECT p1.category FROM places p1 WHERE p1.id = items.placeId))
                       WHERE placeId IN (SELECT id FROM places WHERE category IS NOT NULL)"""
                )
                db.execSQL(
                    """DELETE FROM places WHERE category IS NOT NULL
                       AND id NOT IN (SELECT MIN(id) FROM places WHERE category IS NOT NULL GROUP BY category)"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_places_category ON places(category)")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "roteiro.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
