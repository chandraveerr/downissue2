package com.wynk.download.issue

import android.app.Application
import com.facebook.soloader.SoLoader

/**
 * Created by Prashant Kumar on 5/30/19.
 */
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)
    }
}