/*
 * Copyright (C) 2025  Sergio Camacho
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.secam.simpletag.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.secam.simpletag.data.media.MediaRepo
import dev.secam.simpletag.data.musicbrainz.MusicBrainzApiService
import dev.secam.simpletag.data.musicbrainz.MusicBrainzRepository
import dev.secam.simpletag.data.preferences.PreferencesRepo
import dev.secam.simpletag.data.preferences.PreferencesRepoImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dev.secam.checkin24.user_preferences"
)

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Singleton
    @Provides
    fun providePreferencesRepo(
        @ApplicationContext context: Context
    ): PreferencesRepo = PreferencesRepoImpl(context.dataStore)

    @Singleton
    @Provides
    fun provideMediaRepo(
        @ApplicationContext context: Context
    ): MediaRepo = MediaRepo(context)

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "SimpleTag-Android/0.3.1 ( https://github.com/yourusername/SimpleTag )")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideMusicBrainzApiService(retrofit: Retrofit): MusicBrainzApiService {
        return retrofit.create(MusicBrainzApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideMusicBrainzRepository(apiService: MusicBrainzApiService): MusicBrainzRepository {
        return MusicBrainzRepository(apiService)
    }
}