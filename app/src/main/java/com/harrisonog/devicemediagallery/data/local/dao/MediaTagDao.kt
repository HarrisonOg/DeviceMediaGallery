package com.harrisonog.devicemediagallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harrisonog.devicemediagallery.data.local.entities.MediaTagCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaTagDao {

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN media_tag_cross_ref mtc ON t.id = mtc.tagId
        WHERE mtc.mediaUri = :mediaUri
    """)
    suspend fun getTagsForMedia(mediaUri: String): List<TagEntity>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN media_tag_cross_ref mtc ON t.id = mtc.tagId
        WHERE mtc.mediaUri = :mediaUri
    """)
    fun getTagsForMediaFlow(mediaUri: String): Flow<List<TagEntity>>

    @Query("""
        SELECT DISTINCT mtc.mediaUri FROM media_tag_cross_ref mtc
        WHERE mtc.tagId IN (:tagIds)
    """)
    fun getMediaByTags(tagIds: List<Long>): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaTags(crossRefs: List<MediaTagCrossRef>)

    @Query("DELETE FROM media_tag_cross_ref WHERE mediaUri IN (:mediaUris) AND tagId IN (:tagIds)")
    suspend fun removeMediaTags(mediaUris: List<String>, tagIds: List<Long>)

    @Query("DELETE FROM media_tag_cross_ref WHERE mediaUri = :mediaUri")
    suspend fun removeAllTagsFromMedia(mediaUri: String)

    @Query("DELETE FROM media_tag_cross_ref WHERE mediaUri IN (:mediaUris)")
    suspend fun removeAllTagsFromMediaList(mediaUris: List<String>)
}
