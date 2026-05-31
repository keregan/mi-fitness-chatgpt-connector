package com.example.mifitnesschatgptconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Зачем нужны разрешения Health Connect",
                        style = MaterialTheme.typography.titleLarge
                    )

					Text(
						modifier = Modifier.padding(top = 16.dp),
						text = "Приложение читает данные из Health Connect: шаги, сон, пульс, тренировки, дистанцию и калории. Эти данные нужны, чтобы в дальнейшем передавать их в ChatGPT для анализа активности, восстановления и общего состояния. Данные используются только внутри проекта."
					)
                }
            }
        }
    }
}