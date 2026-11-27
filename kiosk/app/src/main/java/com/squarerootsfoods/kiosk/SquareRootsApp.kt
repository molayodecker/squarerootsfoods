package com.squarerootsfoods.kiosk

import android.app.Application
import com.squarerootsfoods.kiosk.update.UpdateService

class SquareRootsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateService.start(this)
    }
}
