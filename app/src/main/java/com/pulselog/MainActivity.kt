package com.pulselog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.pulselog.data.AppDatabase
import com.pulselog.ui.BloodPressureScreen
import com.pulselog.ui.BpRepository
import com.pulselog.ui.BpViewModel
import com.pulselog.ui.PulseLogTheme

class MainActivity : ComponentActivity() {

    private lateinit var vm: BpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "bp-db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        val repo = BpRepository(db.bpDao())
        vm = BpViewModel(repo)

        setContent {
            PulseLogTheme {
                BloodPressureScreen(vm)
            }
        }
    }
}
