package com.harrisonog.devicemediagallery.di

import com.harrisonog.devicemediagallery.data.repository.MediaRepository
import com.harrisonog.devicemediagallery.data.repository.MediaRepositoryImpl
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
}
