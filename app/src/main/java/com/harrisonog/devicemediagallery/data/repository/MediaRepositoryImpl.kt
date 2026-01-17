package com.harrisonog.devicemediagallery.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import com.harrisonog.devicemediagallery.data.local.dao.MediaTagDao
import com.harrisonog.devicemediagallery.domain.model.MediaFolder
import com.harrisonog.devicemediagallery.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val mediaTagDao: MediaTagDao
) : MediaRepository {

    override fun getMediaItems(folderPath: String?): Flow<List<MediaItem>> = flow {
        val items = queryMediaItems(folderPath)
        emit(items)
    }.flowOn(Dispatchers.IO)

    override fun getFolders(): Flow<List<MediaFolder>> = flow {
        val folders = queryFolders()
        emit(folders)
    }.flowOn(Dispatchers.IO)

    override suspend fun getMediaItemByUri(uri: String): MediaItem? {
        return queryMediaItems().find { it.uri.toString() == uri }
    }

    private suspend fun queryMediaItems(folderPath: String? = null): List<MediaItem> {
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

        val selection = buildString {
            append("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)")

            if (folderPath != null) {
                append(" AND ${MediaStore.Files.FileColumns.DATA} LIKE ?")
            }
        }

        val selectionArgs = mutableListOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        if (folderPath != null) {
            selectionArgs.add("$folderPath/%")
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs.toTypedArray(),
            sortOrder
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
                val mimeType = cursor.getString(mimeTypeColumn) ?: continue
                val path = cursor.getString(dataColumn) ?: continue

                val duration = if (mimeType.startsWith("video/")) {
                    cursor.getLong(durationColumn).takeIf { it > 0 }
                } else null

                val tags = mediaTagDao.getTagsForMedia(uri.toString())
                    .map { it.toDomainModel() }

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
                    duration = duration,
                    tags = tags
                )

                items.add(item)
            }
        }

        return items
    }

    private suspend fun queryFolders(): List<MediaFolder> {
        val folderMap = mutableMapOf<String, Int>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)

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
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: continue
                val folderPath = File(path).parent ?: continue
                folderMap[folderPath] = (folderMap[folderPath] ?: 0) + 1
            }
        }

        return folderMap.map { (path, count) ->
            MediaFolder(
                path = path,
                name = File(path).name,
                mediaCount = count
            )
        }.sortedByDescending { it.mediaCount }
    }
}
