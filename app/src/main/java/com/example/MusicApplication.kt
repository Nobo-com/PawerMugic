package com.example

import android.app.Application
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.StartAppAd

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            StartAppSDK.init(this, "205913194", true)
            StartAppSDK.setTestAdsEnabled(false)
            StartAppAd.disableSplash()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
