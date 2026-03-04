package com.example.bplogger.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun BloodPressureScreen(vm: BpViewModel) {
    var sys by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }

    val records by vm.todayRecords.collectAsState()

    val hasAm = records.any { it.slot == "AM" }
    val hasPm = records.any { it.slot == "PM" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("혈압 기록", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        // ✅ 오늘 완료 상태
        Text("오늘 아침: ${if (hasAm) "✅" else "❌"}   오늘 저녁: ${if (hasPm) "✅" else "❌"}")
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = sys,
            onValueChange = { sys = it.filter(Char::isDigit) },
            label = { Text("수축기") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = dia,
            onValueChange = { dia = it.filter(Char::isDigit) },
            label = { Text("이완기") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        val sysI = sys.toIntOrNull()
        val diaI = dia.toIntOrNull()

        Button(
            onClick = {
                if (sysI == null || diaI == null) return@Button
                vm.saveBp(sysI, diaI, pulse = null, note = null)
                sys = ""
                dia = ""
            },
            enabled = (sysI != null && diaI != null),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }

        Spacer(Modifier.height(20.dp))
        Text("오늘 기록", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (records.isEmpty()) {
            Text("아직 기록이 없어요.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { r ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (r.slot == "AM") "아침" else "저녁")
                            Text("${r.systolic} / ${r.diastolic}")
                        }
                    }
                }
            }
        }
    }
}