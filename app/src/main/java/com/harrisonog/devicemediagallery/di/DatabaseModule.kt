package com.harrisonog.devicemediagallery.di

import android.content.Context
import androidx.room.Room
import com.harrisonog.devicemediagallery.data.local.GalleryDatabase
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

    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase {
        return Room.databaseBuilder(
            context,
            GalleryDatabase::class.java,
            GalleryDatabase.DATABASE_NAME
        ).build()
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
}
