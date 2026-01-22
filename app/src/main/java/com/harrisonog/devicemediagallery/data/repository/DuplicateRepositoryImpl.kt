package com.harrisonog.devicemediagallery.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.harrisonog.devicemediagallery.data.local.dao.DuplicateGroupDao
import com.harrisonog.devicemediagallery.data.local.entities.DuplicateGroupEntity
import com.harrisonog.devicemediagallery.domain.model.DuplicateGroup
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import com.harrisonog.devicemediagallery.util.HashUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DuplicateRepository"

@Singleton
class DuplicateRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val duplicateGroupDao: DuplicateGroupDao
) : DuplicateRepository {

    override fun getDuplicateGroups(): Flow<List<DuplicateGroup>> {
        return duplicateGroupDao.getAllGroups().map { groups ->
            groups.mapNotNull { group ->
                val mediaUris = duplicateGroupDao.getMediaUrisForGroup(group.groupHash)
                val mediaItems = mediaUris.mapNotNull { uriString ->
                    getMediaItemFromUri(uriString)
                }
                if (mediaItems.size >= 2) {
                    DuplicateGroup(
                        groupHash = group.groupHash,
                        mediaItems = mediaItems,
                        detectedAt = group.detectedAt
                    )
                } else {
                    null
                }
            }
        }
    }

    override suspend fun detectDuplicates(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            duplicateGroupDao.clearAllGroups()

            val allMedia = queryAllMedia()
            val hashMap = mutableMapOf<String, MutableList<String>>()

            for (item in allMedia) {
                val hash = HashUtil.calculateMd5FromUri(contentResolver, item.uri)
                if (hash != null) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(item.uri.toString())
                }
            }

            val duplicateGroups = hashMap.filter { it.value.size > 1 }
            var totalDuplicates = 0

            for ((hash, uris) in duplicateGroups) {
                val group = DuplicateGroupEntity(
                    groupHash = hash,
                    detectedAt = System.currentTimeMillis(),
                    groupSize = uris.size
                )
                duplicateGroupDao.insertGroupWithMedia(group, uris)
                totalDuplicates += uris.size
            }

            Result.success(totalDuplicates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDuplicates(items: List<MediaItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (item in items) {
                contentResolver.delete(item.uri, null, null)
                duplicateGroupDao.removeMediaFromGroups(item.uri.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearDuplicateCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            duplicateGroupDao.clearAllGroups()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryAllMedia(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"

        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                val mimeType = cursor.getString(mimeTypeColumn)
                if (mimeType == null) {
                    Log.d(TAG, "Skipping item $id: null mimeType")
                    continue
                }
                val path = cursor.getString(dataColumn)
                if (path == null) {
                    Log.d(TAG, "Skipping item $id: null path")
                    continue
                }

                val duration = if (mimeType.startsWith("video/")) {
                    cursor.getLong(durationColumn).takeIf { it > 0 }
                } else null

                val item = MediaItem(
                    id = id,
                    uri = uri,
                    path = path,
                    fileName = cursor.getString(nameColumn) ?: "",
                    mimeType = mimeType,
                    size = cursor.getLong(sizeColumn),
                    dateAdded = cursor.getLong(dateAddedColumn) * 1000,
                    dateModified = cursor.getLong(dateModifiedColumn) * 1000,
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    duration = duration
                )

                items.add(item)
            }
        }

        return items
    }

    private fun getMediaItemFromUri(uriString: String): MediaItem? {
        val uri = Uri.parse(uriString)
        val id = uri.lastPathSegment?.toLongOrNull()
        if (id == null) {
            Log.w(TAG, "Failed to parse ID from URI: $uriString")
            return null
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                val mimeType = cursor.getString(mimeTypeColumn) ?: return null
                val path = cursor.getString(dataColumn) ?: return null

                val duration = if (mimeType.startsWith("video/")) {
                    cursor.getLong(durationColumn).takeIf { it > 0 }
                } else null

                return MediaItem(
                    id = id,
                    uri = uri,
                    path = path,
                    fileName = cursor.getString(nameColumn) ?: "",
                    mimeType = mimeType,
                    size = cursor.getLong(sizeColumn),
                    dateAdded = cursor.getLong(dateAddedColumn) * 1000,
                    dateModified = cursor.getLong(dateModifiedColumn) * 1000,
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    duration = duration
                )
            }
        }

        return null
    }
}
