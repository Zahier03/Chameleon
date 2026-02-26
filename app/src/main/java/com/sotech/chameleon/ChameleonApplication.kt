package com.sotech.chameleon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChameleonApplication : Application() {
    companion object {
        private const val TAG = "ChameleonApplication"
    }
}