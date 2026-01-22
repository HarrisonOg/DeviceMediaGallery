package com.harrisonog.devicemediagallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.harrisonog.devicemediagallery.data.local.entities.DuplicateGroupEntity
import com.harrisonog.devicemediagallery.data.local.entities.DuplicateGroupMediaCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DuplicateGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCrossRefs(crossRefs: List<DuplicateGroupMediaCrossRef>)

    @Transaction
    suspend fun insertGroupWithMedia(group: DuplicateGroupEntity, mediaUris: List<String>) {
        insertGroup(group)
        val crossRefs = mediaUris.map { uri ->
            DuplicateGroupMediaCrossRef(groupHash = group.groupHash, mediaUri = uri)
        }
        insertMediaCrossRefs(crossRefs)
    }

    @Query("SELECT * FROM duplicate_groups ORDER BY detectedAt DESC")
    fun getAllGroups(): Flow<List<DuplicateGroupEntity>>

    @Query("SELECT mediaUri FROM duplicate_group_media_cross_ref WHERE groupHash = :groupHash")
    suspend fun getMediaUrisForGroup(groupHash: String): List<String>

    @Query("DELETE FROM duplicate_groups WHERE groupHash = :groupHash")
    suspend fun deleteGroup(groupHash: String)

    @Query("DELETE FROM duplicate_group_media_cross_ref WHERE mediaUri = :mediaUri")
    suspend fun removeMediaFromGroups(mediaUri: String)

    @Query("DELETE FROM duplicate_groups")
    suspend fun clearAllGroups()

    @Query("SELECT COUNT(*) FROM duplicate_groups")
    suspend fun getGroupCount(): Int
}
