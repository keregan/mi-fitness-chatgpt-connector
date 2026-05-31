package com.example.mifitnesschatgptconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private var updateText: ((String) -> Unit)? = null

    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->
            if (grantedPermissions.containsAll(permissions)) {
                readTodaySteps()
            } else {
                updateText?.invoke("Разрешение на чтение шагов не выдано.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var resultText by remember {
                mutableStateOf("Нажми кнопку, чтобы прочитать шаги из Health Connect")
            }

            updateText = { newText ->
                resultText = newText
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = resultText)

                    Button(
                        modifier = Modifier.padding(top = 24.dp),
                        onClick = {
                            checkHealthConnectAndPermissions()
                        }
                    ) {
                        Text(text = "Прочитать шаги за сегодня")
                    }
                }
            }
        }
    }

    private fun checkHealthConnectAndPermissions() {
        val status = HealthConnectClient.getSdkStatus(this)

        if (status != HealthConnectClient.SDK_AVAILABLE) {
            updateText?.invoke("Health Connect недоступен или не установлен.")
            return
        }

        val healthConnectClient = HealthConnectClient.getOrCreate(this)

        lifecycleScope.launch {
            val grantedPermissions = healthConnectClient
                .permissionController
                .getGrantedPermissions()

            if (grantedPermissions.containsAll(permissions)) {
                readTodaySteps()
            } else {
                requestPermissions.launch(permissions)
            }
        }
    }

    private fun readTodaySteps() {
        lifecycleScope.launch {
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(this@MainActivity)

                val zoneId = ZoneId.systemDefault()

                val startOfDay = LocalDate.now()
                    .atStartOfDay(zoneId)
                    .toInstant()

                val now = Instant.now()

                val response = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )

                val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L

                updateText?.invoke(
                    """
                    Данные из Health Connect:
                    
                    Шаги за сегодня: $steps
                    """.trimIndent()
                )

            } catch (e: Exception) {
                updateText?.invoke("Ошибка чтения шагов: ${e.message}")
            }
        }
    }
}