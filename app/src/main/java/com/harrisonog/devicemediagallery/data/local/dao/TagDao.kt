package com.harrisonog.devicemediagallery.data.local.dao

import androidx.compose.ui.graphics.Color
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.harrisonog.devicemediagallery.data.local.entities.TagEntity
import com.harrisonog.devicemediagallery.domain.model.Tag
import kotlinx.coroutines.flow.Flow

data class TagWithUsageCount(
    val id: Long,
    val name: String,
    val color: Int,
    val createdAt: Long,
    val usageCount: Int
) {
    fun toDomainModel(): Tag {
        return Tag(
            id = id,
            name = name,
            color = Color(color),
            usageCount = usageCount
        )
    }
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("""
        SELECT t.*, COUNT(mtc.mediaUri) as usageCount
        FROM tags t
        LEFT JOIN media_tag_cross_ref mtc ON t.id = mtc.tagId
        GROUP BY t.id
        ORDER BY t.name ASC
    """)
    fun getAllTagsWithUsageCounts(): Flow<List<TagWithUsageCount>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long)

    @Query("SELECT COUNT(*) FROM media_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getTagUsageCount(tagId: Long): Int
}
