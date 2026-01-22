package com.harrisonog.devicemediagallery.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.harrisonog.devicemediagallery.data.local.GalleryDatabase
import com.harrisonog.devicemediagallery.data.local.dao.DuplicateGroupDao
import com.harrisonog.devicemediagallery.data.local.dao.MediaTagDao
import com.harrisonog.devicemediagallery.data.local.dao.TagDao
import com.harrisonog.devicemediagallery.data.local.dao.TrashItemDao
import com.harrisonog.devicemediagallery.data.local.dao.VirtualAlbumDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `duplicate_groups` (
                    `groupHash` TEXT NOT NULL,
                    `detectedAt` INTEGER NOT NULL,
                    `groupSize` INTEGER NOT NULL,
                    PRIMARY KEY(`groupHash`)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `duplicate_group_media_cross_ref` (
                    `groupHash` TEXT NOT NULL,
                    `mediaUri` TEXT NOT NULL,
                    PRIMARY KEY(`groupHash`, `mediaUri`),
                    FOREIGN KEY(`groupHash`) REFERENCES `duplicate_groups`(`groupHash`) ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_duplicate_group_media_cross_ref_groupHash` ON `duplicate_group_media_cross_ref` (`groupHash`)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add index on virtual_albums.updatedAt for sorting
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_virtual_albums_updatedAt` ON `virtual_albums` (`updatedAt`)")

            // Add index on tags.name for search/lookup
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

            // Add indices on trash_items for auto-delete and lookup
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_autoDeleteAt` ON `trash_items` (`autoDeleteAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_trash_items_originalUri` ON `trash_items` (`originalUri`)")
        }
    }

    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase {
        return Room.databaseBuilder(
            context,
            GalleryDatabase::class.java,
            GalleryDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideVirtualAlbumDao(database: GalleryDatabase): VirtualAlbumDao {
        return database.virtualAlbumDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: GalleryDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideMediaTagDao(database: GalleryDatabase): MediaTagDao {
        return database.mediaTagDao()
    }

    @Provides
    @Singleton
    fun provideTrashItemDao(database: GalleryDatabase): TrashItemDao {
        return database.trashItemDao()
    }

    @Provides
    @Singleton
    fun provideDuplicateGroupDao(database: GalleryDatabase): DuplicateGroupDao {
        return database.duplicateGroupDao()
    }
}
