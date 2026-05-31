package com.example.mifitnesschatgptconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    private var updateData: ((HealthUiData) -> Unit)? = null

    private val requestPermissions =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->
            if (grantedPermissions.containsAll(permissions)) {
                readTodayHealthData()
            } else {
                updateData?.invoke(
                    HealthUiData(
                        status = "Выданы не все разрешения Health Connect. Проверь доступ к шагам, сну, пульсу, тренировкам, дистанции и калориям."
                    )
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var uiData by remember {
                mutableStateOf(
                    HealthUiData(
                        status = "Нажми кнопку, чтобы прочитать данные из Health Connect"
                    )
                )
            }

            updateData = { newData ->
                uiData = newData
            }

            MaterialTheme {
                HealthScreen(
                    data = uiData,
                    onReadClick = {
                        checkHealthConnectAndPermissions()
                    }
                )
            }
        }
    }

    private fun checkHealthConnectAndPermissions() {
        val status = HealthConnectClient.getSdkStatus(this)

        if (status != HealthConnectClient.SDK_AVAILABLE) {
            updateData?.invoke(
                HealthUiData(
                    status = "Health Connect недоступен или не установлен."
                )
            )
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

                val heartRateAvg = aggregateResponse[HeartRateRecord.BPM_AVG]?.toString()
                val heartRateMin = aggregateResponse[HeartRateRecord.BPM_MIN]?.toString()
                val heartRateMax = aggregateResponse[HeartRateRecord.BPM_MAX]?.toString()

                val distanceMeters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]
                    ?.inMeters
                    ?.toInt()
                    ?.toString()

                val activeCaloriesKcal = aggregateResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    ?.inKilocalories
                    ?.toInt()
                    ?.toString()

                val totalCaloriesKcal = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                    ?.inKilocalories
                    ?.toInt()
                    ?.toString()

                val sleepResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )

                val sleepSummary = buildSleepSummary(sleepResponse.records)

                val exerciseResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )

                val workouts = if (exerciseResponse.records.isEmpty()) {
                    "нет данных"
                } else {
                    exerciseResponse.records.joinToString(separator = "\n") { record ->
                        val durationMinutes = Duration.between(
                            record.startTime,
                            record.endTime
                        ).toMinutes()

                        "type=${record.exerciseType}, $durationMinutes мин"
                    }
                }

                updateData?.invoke(
                    HealthUiData(
                        status = null,
                        steps = steps.toString(),
                        sleepTotal = sleepSummary.total,
                        sleepFast = sleepSummary.fast,
                        sleepDeep = sleepSummary.deep,
                        sleepLight = sleepSummary.light,
                        pulseAvg = heartRateAvg ?: "нет данных",
                        pulseMin = heartRateMin ?: "нет данных",
                        pulseMax = heartRateMax ?: "нет данных",
                        distance = distanceMeters ?: "нет данных",
                        activeCalories = activeCaloriesKcal ?: "нет данных",
                        totalCalories = totalCaloriesKcal ?: "нет данных",
                        workouts = workouts
                    )
                )

            } catch (e: Exception) {
                updateData?.invoke(
                    HealthUiData(
                        status = "Ошибка чтения данных: ${e.message}"
                    )
                )
            }
        }
    }

    private fun buildSleepSummary(records: List<SleepSessionRecord>): SleepSummary {
        if (records.isEmpty()) {
            return SleepSummary(
                total = "нет данных",
                fast = "нет данных",
                deep = "нет данных",
                light = "нет данных"
            )
        }

        var totalMinutes = 0L
        var remMinutes = 0L
        var deepMinutes = 0L
        var lightMinutes = 0L

        records.forEach { record ->
            totalMinutes += Duration.between(record.startTime, record.endTime).toMinutes()

            record.stages.forEach { stage ->
                val minutes = Duration.between(stage.startTime, stage.endTime).toMinutes()

                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_REM -> remMinutes += minutes
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMinutes += minutes
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMinutes += minutes
                }
            }
        }

        return SleepSummary(
            total = formatMinutes(totalMinutes),
            fast = formatMinutes(remMinutes),
            deep = formatMinutes(deepMinutes),
            light = formatMinutes(lightMinutes)
        )
    }

    private fun formatMinutes(minutes: Long): String {
        if (minutes <= 0) return "нет данных"

        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 && remainingMinutes > 0 -> "$hours ч $remainingMinutes мин"
            hours > 0 -> "$hours ч"
            else -> "$remainingMinutes мин"
        }
    }
}

data class HealthUiData(
    val status: String? = null,
    val steps: String = "—",
    val sleepTotal: String = "—",
    val sleepFast: String = "—",
    val sleepDeep: String = "—",
    val sleepLight: String = "—",
    val pulseAvg: String = "—",
    val pulseMin: String = "—",
    val pulseMax: String = "—",
    val distance: String = "—",
    val activeCalories: String = "—",
    val totalCalories: String = "—",
    val workouts: String = "—"
)

data class SleepSummary(
    val total: String,
    val fast: String,
    val deep: String,
    val light: String
)

@Composable
fun HealthScreen(
    data: HealthUiData,
    onReadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Health Connect",
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Данные за сегодня",
            fontSize = 14.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (data.status != null) {
                    Text(
                        text = data.status,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                } else {
                    CompactSection(title = "АКТИВНОСТЬ") {
                        CompactRow(label = "Шаги", value = data.steps)
                    }

                    CompactSection(title = "СОН") {
                        CompactRow(label = "Всего", value = data.sleepTotal)
                        CompactRow(label = "Быстрый", value = data.sleepFast)
                        CompactRow(label = "Крепкий", value = data.sleepDeep)
                        CompactRow(label = "Поверхностный", value = data.sleepLight)
                    }

                    CompactSection(title = "ПУЛЬС") {
                        CompactRow(label = "Средний", value = data.pulseAvg)
                        CompactRow(label = "Минимальный", value = data.pulseMin)
                        CompactRow(label = "Максимальный", value = data.pulseMax)
                    }

                    CompactSection(title = "ДИСТАНЦИЯ") {
                        CompactRow(label = "Метры", value = data.distance)
                    }

                    CompactSection(title = "КАЛОРИИ") {
                        CompactRow(label = "Активные", value = "${data.activeCalories} ккал")
                        CompactRow(label = "Общие", value = "${data.totalCalories} ккал")
                    }

                    CompactSection(title = "ТРЕНИРОВКИ", bottomSpace = 0.dp) {
                        Text(
                            text = data.workouts,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            onClick = onReadClick
        ) {
            Text(
                text = "Прочитать данные за сегодня",
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun CompactSection(
    title: String,
    bottomSpace: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        fontSize = 13.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )

    content()

    Spacer(modifier = Modifier.height(bottomSpace))
}

@Composable
fun CompactRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}
