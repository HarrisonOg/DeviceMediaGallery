package com.harrisonog.devicemediagallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.harrisonog.devicemediagallery.data.local.dao.MediaTagDao
import com.harrisonog.devicemediagallery.data.local.dao.TagDao
import com.harrisonog.devicemediagallery.data.local.dao.TrashItemDao
import com.harrisonog.devicemediagallery.data.local.dao.VirtualAlbumDao
import com.harrisonog.devicemediagallery.data.local.entities.AlbumMediaCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.MediaTagCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.TagEntity
import com.harrisonog.devicemediagallery.data.local.entities.TrashItemEntity
import com.harrisonog.devicemediagallery.data.local.entities.VirtualAlbumEntity

@Database(
    entities = [
        VirtualAlbumEntity::class,
        TagEntity::class,
        MediaTagCrossRef::class,
        AlbumMediaCrossRef::class,
        TrashItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun virtualAlbumDao(): VirtualAlbumDao
    abstract fun tagDao(): TagDao
    abstract fun mediaTagDao(): MediaTagDao
    abstract fun trashItemDao(): TrashItemDao

    companion object {
        const val DATABASE_NAME = "gallery_database"
    }
}
