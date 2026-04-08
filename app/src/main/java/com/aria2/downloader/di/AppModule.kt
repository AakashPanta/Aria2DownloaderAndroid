package com.aria2.downloader.di

import android.content.Context
import com.aria2.downloader.data.local.AppDatabase
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideDownloadRepository(
        database: AppDatabase
    ): DownloadRepository {
        return DownloadRepository(database)
    }

    @Singleton
    @Provides
    fun provideDownloadEngine(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): DownloadEngine {
        return DownloadEngine(context, okHttpClient, maxConnections = 4)
    }
}
