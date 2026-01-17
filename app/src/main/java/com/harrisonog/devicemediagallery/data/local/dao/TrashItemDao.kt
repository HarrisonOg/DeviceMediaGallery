package com.harrisonog.devicemediagallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harrisonog.devicemediagallery.data.local.entities.TrashItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashItemDao {

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items WHERE originalUri = :originalUri")
    suspend fun getById(originalUri: String): TrashItemEntity?

    @Query("SELECT COUNT(*) FROM trash_items")
    suspend fun getTrashCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrashItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TrashItemEntity>)

    @Query("DELETE FROM trash_items WHERE originalUri = :originalUri")
    suspend fun deleteById(originalUri: String)

    @Query("DELETE FROM trash_items WHERE originalUri IN (:originalUris)")
    suspend fun deleteByIds(originalUris: List<String>)

    @Query("DELETE FROM trash_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM trash_items WHERE autoDeleteAt <= :currentTime")
    suspend fun getExpiredItems(currentTime: Long): List<TrashItemEntity>

    @Query("DELETE FROM trash_items WHERE autoDeleteAt <= :currentTime")
    suspend fun deleteExpired(currentTime: Long): Int
}
