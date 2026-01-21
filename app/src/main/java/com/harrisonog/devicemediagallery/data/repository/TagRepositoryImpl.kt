package com.harrisonog.devicemediagallery.data.repository

import com.harrisonog.devicemediagallery.data.local.dao.MediaTagDao
import com.harrisonog.devicemediagallery.data.local.dao.TagDao
import com.harrisonog.devicemediagallery.data.local.entities.MediaTagCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.TagEntity
import com.harrisonog.devicemediagallery.domain.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val mediaTagDao: MediaTagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags()
            .map { entities ->
                entities.map { entity ->
                    val usageCount = tagDao.getTagUsageCount(entity.id)
                    entity.toDomainModel(usageCount)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTagById(id: Long): Tag? {
        val entity = tagDao.getTagById(id) ?: return null
        val usageCount = tagDao.getTagUsageCount(id)
        return entity.toDomainModel(usageCount)
    }

    override suspend fun createTag(name: String, color: Int): Long {
        val entity = TagEntity(name = name, color = color)
        return tagDao.insert(entity)
    }

    override suspend fun updateTag(tag: Tag) {
        val entity = TagEntity(
            id = tag.id,
            name = tag.name,
            color = tag.color.hashCode()
        )
        tagDao.update(entity)
    }

    override suspend fun deleteTag(id: Long) {
        tagDao.deleteById(id)
    }

    override suspend fun applyTagsToMedia(mediaUris: List<String>, tagIds: List<Long>) {
        val crossRefs = mediaUris.flatMap { uri ->
            tagIds.map { tagId ->
                MediaTagCrossRef(mediaUri = uri, tagId = tagId)
            }
        }
        mediaTagDao.insertMediaTags(crossRefs)
    }

    override suspend fun removeTagsFromMedia(mediaUris: List<String>, tagIds: List<Long>) {
        mediaTagDao.removeMediaTags(mediaUris, tagIds)
    }

    override fun getTagsForMedia(mediaUri: String): Flow<List<Tag>> {
        return mediaTagDao.getTagsForMediaFlow(mediaUri)
            .map { entities ->
                entities.map { entity ->
                    val usageCount = tagDao.getTagUsageCount(entity.id)
                    entity.toDomainModel(usageCount)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getMediaByTags(tagIds: List<Long>): Flow<List<String>> {
        return mediaTagDao.getMediaByTags(tagIds)
            .flowOn(Dispatchers.IO)
    }
}
