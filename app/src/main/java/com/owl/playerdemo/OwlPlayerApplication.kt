package com.owl.playerdemo

import android.app.Application
import com.owl.playerdemo.data.util.NetworkConnectivityManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OwlPlayerApplication : Application() {
    
    @Inject
    lateinit var networkConnectivityManager: NetworkConnectivityManager
    
    override fun onTerminate() {
        super.onTerminate()
        // Clean up network connectivity callbacks
        networkConnectivityManager.unregisterNetworkCallback()
    }
} 