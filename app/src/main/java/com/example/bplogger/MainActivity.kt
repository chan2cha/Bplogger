package com.example.bplogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.room.Room
import com.example.bplogger.data.AppDatabase
import com.example.bplogger.ui.BloodPressureScreen
import com.example.bplogger.ui.BpRepository
import com.example.bplogger.ui.BpViewModel

class MainActivity : ComponentActivity() {

    private lateinit var vm: BpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "bp-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val repo = BpRepository(db.bpDao())
        vm = BpViewModel(repo)

        setContent {
            MaterialTheme {
                BloodPressureScreen(vm)
            }
        }
    }
}
