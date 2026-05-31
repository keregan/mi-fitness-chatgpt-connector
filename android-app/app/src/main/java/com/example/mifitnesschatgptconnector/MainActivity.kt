package com.example.mifitnesschatgptconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    private var updateText: ((String) -> Unit)? = null

    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->
            if (grantedPermissions.containsAll(permissions)) {
                readTodayHealthData()
            } else {
                updateText?.invoke("Выданы не все разрешения Health Connect. Проверь доступ к шагам, сну, пульсу, тренировкам, дистанции и калориям.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var resultText by remember {
                mutableStateOf("Нажми кнопку, чтобы прочитать данные из Health Connect")
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
                        Text(text = "Прочитать данные за сегодня")
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
                readTodayHealthData()
            } else {
                requestPermissions.launch(permissions)
            }
        }
    }

    private fun readTodayHealthData() {
        lifecycleScope.launch {
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(this@MainActivity)

                val zoneId = ZoneId.systemDefault()

                val startOfDay = LocalDate.now()
                    .atStartOfDay(zoneId)
                    .toInstant()

                val now = Instant.now()

                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(
                            StepsRecord.COUNT_TOTAL,
                            SleepSessionRecord.SLEEP_DURATION_TOTAL,
                            HeartRateRecord.BPM_AVG,
                            HeartRateRecord.BPM_MIN,
                            HeartRateRecord.BPM_MAX,
                            DistanceRecord.DISTANCE_TOTAL,
                            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                            TotalCaloriesBurnedRecord.ENERGY_TOTAL
                        ),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )

                val steps = aggregateResponse[StepsRecord.COUNT_TOTAL] ?: 0L

                val sleepDuration = aggregateResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL]
                val sleepMinutes = sleepDuration?.toMinutes() ?: 0L

                val heartRateAvg = aggregateResponse[HeartRateRecord.BPM_AVG]
                val heartRateMin = aggregateResponse[HeartRateRecord.BPM_MIN]
                val heartRateMax = aggregateResponse[HeartRateRecord.BPM_MAX]

                val distance = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]
                val distanceMeters = distance?.inMeters?.toInt()

                val activeCalories = aggregateResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                val activeCaloriesKcal = activeCalories?.inKilocalories?.toInt()

                val totalCalories = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                val totalCaloriesKcal = totalCalories?.inKilocalories?.toInt()

                val exerciseResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )

                val workoutsText = if (exerciseResponse.records.isEmpty()) {
                    "нет данных"
                } else {
                    exerciseResponse.records.joinToString(separator = "\n") { record ->
                        val durationMinutes = Duration.between(
                            record.startTime,
                            record.endTime
                        ).toMinutes()

                        "- тренировка type=${record.exerciseType}, $durationMinutes мин"
                    }
                }

                updateText?.invoke(
                    """
                    Данные из Health Connect за сегодня:
                    
                    Шаги: $steps
                    Сон: $sleepMinutes минут
                    
                    Пульс:
                    Средний: ${heartRateAvg ?: "нет данных"}
                    Минимальный: ${heartRateMin ?: "нет данных"}
                    Максимальный: ${heartRateMax ?: "нет данных"}
                    
                    Дистанция: ${distanceMeters ?: "нет данных"} м
                    
                    Калории:
                    Активные: ${activeCaloriesKcal ?: "нет данных"} ккал
                    Общие: ${totalCaloriesKcal ?: "нет данных"} ккал
                    
                    Тренировки:
                    $workoutsText
                    """.trimIndent()
                )

            } catch (e: Exception) {
                updateText?.invoke("Ошибка чтения данных: ${e.message}")
            }
        }
    }
}