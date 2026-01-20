package com.example.fantasyfootballapp

import android.app.Application
import com.example.fantasyfootballapp.network.ApiClient

class FantasyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}