package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun getTagById(id: Long): Tag?
    suspend fun createTag(name: String, color: Int): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(id: Long)
    suspend fun applyTagsToMedia(mediaUris: List<String>, tagIds: List<Long>)
    suspend fun removeTagsFromMedia(mediaUris: List<String>, tagIds: List<Long>)
    fun getTagsForMedia(mediaUri: String): Flow<List<Tag>>
    fun getMediaByTags(tagIds: List<Long>): Flow<List<String>>
}
