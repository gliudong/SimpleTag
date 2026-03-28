
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
import dev.secam.simpletag.data.musicbrainz.CoverArtArchiveApiService
import dev.secam.simpletag.data.musicbrainz.CoverArtRepository
import dev.secam.simpletag.data.musicbrainz.MusicBrainzApiService
import dev.secam.simpletag.data.musicbrainz.MusicBrainzRepository
import dev.secam.simpletag.data.preferences.PreferencesRepo
import dev.secam.simpletag.data.preferences.PreferencesRepoImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dev.secam.checkin24.user_preferences"
)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MusicBrainzRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoverArtArchiveRetrofit

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
    @MusicBrainzRetrofit
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideMusicBrainzApiService(@MusicBrainzRetrofit retrofit: Retrofit): MusicBrainzApiService {
        return retrofit.create(MusicBrainzApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideMusicBrainzRepository(apiService: MusicBrainzApiService): MusicBrainzRepository {
        return MusicBrainzRepository(apiService)
    }

    @Singleton
    @Provides
    @CoverArtArchiveRetrofit
    fun provideCoverArtArchiveRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://coverartarchive.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideCoverArtArchiveApiService(@CoverArtArchiveRetrofit retrofit: Retrofit): CoverArtArchiveApiService {
        return retrofit.create(CoverArtArchiveApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideCoverArtRepository(apiService: CoverArtArchiveApiService): CoverArtRepository {
        return CoverArtRepository(apiService)
    }
}