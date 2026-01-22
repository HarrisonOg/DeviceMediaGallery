package com.harrisonog.devicemediagallery.di

import com.harrisonog.devicemediagallery.data.repository.AlbumRepository
import com.harrisonog.devicemediagallery.data.repository.AlbumRepositoryImpl
import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.data.repository.MediaRepositoryImpl
import com.harrisonog.devicemediagallery.data.repository.TagRepository
import com.harrisonog.devicemediagallery.data.repository.TagRepositoryImpl
import com.harrisonog.devicemediagallery.data.repository.TrashRepository
import com.harrisonog.devicemediagallery.data.repository.TrashRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(
        impl: AlbumRepositoryImpl
    ): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        impl: TagRepositoryImpl
    ): TagRepository

    @Binds
    @Singleton
    abstract fun bindTrashRepository(
        impl: TrashRepositoryImpl
    ): TrashRepository
}
