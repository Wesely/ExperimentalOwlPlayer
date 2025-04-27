package com.owl.playerdemo.di

import android.content.Context
import com.owl.playerdemo.data.util.NetworkConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Add other application-wide dependencies here
    
    @Provides
    @Singleton
    fun provideNetworkConnectivityManager(
        @ApplicationContext context: Context
    ): NetworkConnectivityManager {
        return NetworkConnectivityManager(context)
    }
} 