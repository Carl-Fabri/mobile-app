package com.example.utpstudywork.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.utpstudywork.domain.model.SessionType

private fun Int.formatAsTime(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun HomeScreen(vm: HomeViewModel, onNavigateToNotes: () -> Unit = {}) {
    val ui = vm.state.collectAsState().value
    HomeContent(
        state = ui,
        onStart = vm::startTimer,
        onPause = vm::pauseTimer,
        onStop = vm::stopTimer,
        onSwitchTab = vm::switchTab,
        onNavigateToNotes = onNavigateToNotes
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSwitchTab: (SessionType) -> Unit,
    onNavigateToNotes: () -> Unit = {}
) {
    val tabIndex = if (state.timer.type == SessionType.WORK) 0 else 1
    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true,  onClick = {}, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Inicio") })
                NavigationBarItem(selected = false, onClick = onNavigateToNotes, icon = { Icon(Icons.Filled.EventNote, null) }, label = { Text("Notas") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.BarChart, null) }, label = { Text("Stats") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Config") })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val dateStr = try {
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es")))
            } catch (e: Exception) { "" }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("StudyWork Pro", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text("¡Hola! Hoy es $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(16.dp))

            TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Tab(selected = tabIndex == 0, onClick = { onSwitchTab(SessionType.WORK) }, text = { Text("Trabajo") })
                Tab(selected = tabIndex == 1, onClick = { onSwitchTab(SessionType.STUDY) }, text = { Text("Estudio") })
            }

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B6CEE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.timer.type == SessionType.WORK) "Sesión de Trabajo" else "Sesión de Estudio",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = state.timer.remainingSec.formatAsTime(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onStart,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Iniciar", modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        FilledTonalIconButton(
                            onClick = onPause,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Pause, "Pausar", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Progreso de Hoy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            ProgressItem(label = "Trabajo", minutes = state.workMinToday)
            Spacer(Modifier.height(12.dp))
            ProgressItem(label = "Estudio", minutes = state.studyMinToday)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notas Recientes", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNavigateToNotes) {
                    Text("Ver todas", style = MaterialTheme.typography.labelSmall, color = primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.upcomingNotes.isEmpty()) {
                Spacer(Modifier.height(8.dp))
            } else {
                state.upcomingNotes.forEach { note ->
                    NoteItem(title = note.title, subtitle = note.description, color = note.color)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProgressItem(label: String, minutes: Int) {
    val goal = 60 * 4
    val ratio = (minutes.toFloat() / goal).coerceIn(0f, 1f)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${minutes.asHm()}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun NoteItem(title: String, subtitle: String, color: Int? = null) {
    val container = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Int.asHm(): String = "${this / 60}h ${this % 60}m"

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HomeContent(
        state = HomeUiState(
            timer = TimerUi(),
            workMinToday = 150,
            studyMinToday = 105
        ),
        onStart = {},
        onPause = {},
        onStop = {},
        onSwitchTab = {},
        onNavigateToNotes = {}
    )
}