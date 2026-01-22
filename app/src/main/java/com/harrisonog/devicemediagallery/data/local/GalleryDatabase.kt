package com.harrisonog.devicemediagallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.harrisonog.devicemediagallery.data.local.dao.DuplicateGroupDao
import com.harrisonog.devicemediagallery.data.local.dao.MediaTagDao
import com.harrisonog.devicemediagallery.data.local.dao.TagDao
import com.harrisonog.devicemediagallery.data.local.dao.TrashItemDao
import com.harrisonog.devicemediagallery.data.local.dao.VirtualAlbumDao
import com.harrisonog.devicemediagallery.data.local.entities.AlbumMediaCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.DuplicateGroupEntity
import com.harrisonog.devicemediagallery.data.local.entities.DuplicateGroupMediaCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.MediaTagCrossRef
import com.harrisonog.devicemediagallery.data.local.entities.TagEntity
import com.harrisonog.devicemediagallery.data.local.entities.TrashItemEntity
import com.harrisonog.devicemediagallery.data.local.entities.VirtualAlbumEntity

// Note: exportSchema = true enables schema export for future auto-migrations.
// Auto-migrations can be added when schema files exist:
// autoMigrations = [AutoMigration(from = 3, to = 4)]
@Database(
    entities = [
        VirtualAlbumEntity::class,
        TagEntity::class,
        MediaTagCrossRef::class,
        AlbumMediaCrossRef::class,
        TrashItemEntity::class,
        DuplicateGroupEntity::class,
        DuplicateGroupMediaCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun virtualAlbumDao(): VirtualAlbumDao
    abstract fun tagDao(): TagDao
    abstract fun mediaTagDao(): MediaTagDao
    abstract fun trashItemDao(): TrashItemDao
    abstract fun duplicateGroupDao(): DuplicateGroupDao

    companion object {
        const val DATABASE_NAME = "gallery_database"
    }
}
