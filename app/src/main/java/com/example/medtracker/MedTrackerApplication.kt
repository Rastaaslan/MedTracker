package com.example.medtracker

import android.app.Application
import com.example.medtracker.data.*

class MedTrackerApplication:Application(){
    val database by lazy { MedTrackerDatabase.create(this) }
    val repository by lazy { MedTrackerRepository(database.dao()) }
}
