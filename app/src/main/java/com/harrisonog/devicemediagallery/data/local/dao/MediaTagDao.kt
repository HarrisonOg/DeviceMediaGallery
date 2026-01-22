package com.harrisonog.devicemediagallery.data.local.dao

import androidx.compose.ui.graphics.Color
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harrisonog.devicemediagallery.data.local.entities.MediaTagCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.TagEntity
import com.harrisonog.devicemediagallery.domain.model.Tag
import kotlinx.coroutines.flow.Flow

data class MediaTagResult(
    val mediaUri: String,
    val tagId: Long,
    val tagName: String,
    val tagColor: Int
) {
    fun toDomainModel(): Tag {
        return Tag(
            id = tagId,
            name = tagName,
            color = Color(tagColor),
            usageCount = 0
        )
    }
}

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
        SELECT t.*, COUNT(mtc2.mediaUri) as usageCount
        FROM tags t
        INNER JOIN media_tag_cross_ref mtc ON t.id = mtc.tagId
        LEFT JOIN media_tag_cross_ref mtc2 ON t.id = mtc2.tagId
        WHERE mtc.mediaUri = :mediaUri
        GROUP BY t.id
    """)
    fun getTagsForMediaWithUsageCount(mediaUri: String): Flow<List<TagWithUsageCount>>

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

    @Query("""
        SELECT mtc.mediaUri, t.id as tagId, t.name as tagName, t.color as tagColor
        FROM media_tag_cross_ref mtc
        INNER JOIN tags t ON mtc.tagId = t.id
        WHERE mtc.mediaUri IN (:mediaUris)
    """)
    suspend fun getTagsForMediaUris(mediaUris: List<String>): List<MediaTagResult>
}
