package com.example.utpstudywork.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.SessionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Tokens de diseño ──────────────────────────────────────────────────────────

private val CARD_RADIUS   = RoundedCornerShape(24.dp)
private val ITEM_RADIUS   = RoundedCornerShape(20.dp)
private val PILL_RADIUS   = RoundedCornerShape(50.dp)
private val COLOR_TIMER   = Color(0xFF1B2A6B)     // azul profundo para el timer
private val COLOR_TIMER2  = Color(0xFF2B3FA0)
private val COLOR_WORK    = Color(0xFF4F7CF6)
private val COLOR_STUDY   = Color(0xFF7C5DF6)
private val ALPHA_SURFACE = 0.06f

// ── Utils ─────────────────────────────────────────────────────────────────────

private fun Int.formatAsTime(): String {
    val m = this / 60; val s = this % 60
    return String.format("%02d:%02d", m, s)
}

private fun Int.asHm(): String = if (this == 0) "0m" else
    listOfNotNull(
        if (this / 60 > 0) "${this / 60}h" else null,
        if (this % 60 > 0) "${this % 60}m" else null
    ).joinToString(" ")

// ── Pantalla ─────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onNavigateToNotes: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {}
) {
    val ui = vm.state.collectAsState().value
    HomeContent(
        state = ui,
        onStart = vm::startTimer,
        onPause = vm::pauseTimer,
        onStop = vm::stopTimer,
        onSwitchTab = vm::switchTab,
        onToggleFocus = vm::toggleFocus,
        onSetNoteStatus = vm::setNoteStatus,
        onToggleTodayNotes = vm::toggleShowTodayNotes,
        onNavigateToNotes = onNavigateToNotes,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToPlanner = onNavigateToPlanner
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSwitchTab: (SessionType) -> Unit,
    onToggleFocus: (Note) -> Unit,
    onSetNoteStatus: (Note, NoteStatus) -> Unit = { _, _ -> },
    onToggleTodayNotes: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {}
) {
    val isWork = state.timer.type == SessionType.WORK

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppBottomBar(selected = 0, onNavigateToNotes, onNavigateToNotifications, onNavigateToPlanner) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Cabecera ─────────────────────────────────────────────────────
            val dateStr = try {
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es")))
                    .replaceFirstChar { it.uppercase() }
            } catch (_: Exception) { "" }

            Text(
                "StudyWork",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // ── Selector de sesión (pill segmentado) ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(PILL_RADIUS)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SessionTab(
                        label = "Trabajo",
                        selected = isWork,
                        activeColor = COLOR_WORK,
                        modifier = Modifier.weight(1f)
                    ) { onSwitchTab(SessionType.WORK) }
                    SessionTab(
                        label = "Estudio",
                        selected = !isWork,
                        activeColor = COLOR_STUDY,
                        modifier = Modifier.weight(1f)
                    ) { onSwitchTab(SessionType.STUDY) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Timer card ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CARD_RADIUS)
                    .background(Brush.verticalGradient(listOf(COLOR_TIMER, COLOR_TIMER2)))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Dot + label de sesión
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.timer.running) Color(0xFF4EF0A3) else Color(0xFFFFFFFF).copy(alpha = 0.4f))
                        )
                        Text(
                            if (isWork) "Sesión de Trabajo" else "Sesión de Estudio",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Tiempo
                    Text(
                        state.timer.remainingSec.formatAsTime(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 72.sp,
                            letterSpacing = (-2).sp
                        ),
                        color = Color.White
                    )

                    // Notas activas en el timer
                    if (state.activeNotes.isNotEmpty()) {
                        Text(
                            state.activeNotes.joinToString("  ·  ") { it.title },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Botones de control
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerButton(
                            icon = Icons.Filled.Stop,
                            label = "Detener",
                            onClick = onStop,
                            tint = Color.White.copy(alpha = 0.6f),
                            size = 48.dp
                        )
                        TimerButton(
                            icon = if (state.timer.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            label = if (state.timer.running) "Pausar" else "Iniciar",
                            onClick = if (state.timer.running) onPause else onStart,
                            tint = Color.White,
                            size = 64.dp,
                            primary = true
                        )
                        // spacer para simetría
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }

            // ── En progreso ──────────────────────────────────────────────────
            if (state.activeNotes.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionHeader(title = "En progreso") {
                    TextButton(onClick = onNavigateToNotes) { Text("Ver notas") }
                }
                Spacer(Modifier.height(10.dp))
                state.activeNotes.forEach { note ->
                    ActiveNoteCard(note = note, onSetStatus = { s -> onSetNoteStatus(note, s) })
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Notas Pendientes ─────────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            SectionHeader(
                title = "Notas Pendientes",
                subtitle = "Toca ○ para activar en el Pomodoro"
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = PILL_RADIUS
                ) {
                    Text(
                        "${state.focusedNotes.size}/2",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (state.pendingNotes.isEmpty()) {
                EmptyState("No tienes notas pendientes")
            } else {
                state.pendingNotes.forEach { note ->
                    FocusNoteItem(
                        note = note,
                        isFocused = note.isFocused,
                        canFocus = note.isFocused || state.focusedNotes.size < 2,
                        onToggle = { onToggleFocus(note) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Progreso de Hoy ──────────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            SectionHeader(title = "Progreso de Hoy") {
                if (state.todayNotes.isNotEmpty()) {
                    TextButton(onClick = onToggleTodayNotes) {
                        Icon(
                            imageVector = if (state.showTodayNotes) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.showTodayNotes) "Ocultar" else "Ver notas")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CARD_RADIUS)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProgressRow(
                        label = "Trabajo",
                        minutes = state.workMinToday,
                        color = COLOR_WORK
                    )
                    ProgressRow(
                        label = "Estudio",
                        minutes = state.studyMinToday,
                        color = COLOR_STUDY
                    )
                    if (state.todayNotes.isNotEmpty()) {
                        val completed = state.todayNotes.count { it.status == NoteStatus.COMPLETED }
                        ProgressRow(
                            label = "Notas de hoy",
                            minutes = -1,
                            color = MaterialTheme.colorScheme.tertiary,
                            ratio = completed.toFloat() / state.todayNotes.size,
                            overrideLabel = "$completed / ${state.todayNotes.size} completadas"
                        )
                    }
                }
            }

            // Lista expandible de notas de hoy
            AnimatedVisibility(
                visible = state.showTodayNotes && state.todayNotes.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    state.todayNotes.forEach { note ->
                        TodayNoteItem(note)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun SessionTab(
    label: String,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(PILL_RADIUS)
            .background(if (selected) activeColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    primary: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(if (primary) 32.dp else 22.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActiveNoteCard(note: Note, onSetStatus: (NoteStatus) -> Unit = {}) {
    val doneCount = note.tasks.count { it.isDone }
    val totalTasks = note.tasks.size
    val accentColor = when (note.status) {
        NoteStatus.ACTIVE    -> MaterialTheme.colorScheme.primary
        NoteStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        NoteStatus.PENDING   -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ITEM_RADIUS,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(note.color).copy(alpha = 0.85f))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Acento de color de estado
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        if (note.description.isNotBlank()) {
                            Text(
                                note.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    StatusPill(note.status)
                }
                if (totalTasks > 0) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / totalTasks },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(PILL_RADIUS),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$doneCount de $totalTasks tareas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoteStatus.entries.forEach { status ->
                        val label = when (status) {
                            NoteStatus.PENDING   -> "Pendiente"
                            NoteStatus.ACTIVE    -> "Activa"
                            NoteStatus.COMPLETED -> "Lista"
                        }
                        FilterChip(
                            selected = note.status == status,
                            onClick = { if (note.status != status) onSetStatus(status) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = PILL_RADIUS
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: NoteStatus) {
    val (label, color) = when (status) {
        NoteStatus.PENDING   -> "Pendiente" to MaterialTheme.colorScheme.outline
        NoteStatus.ACTIVE    -> "Activa"    to MaterialTheme.colorScheme.primary
        NoteStatus.COMPLETED -> "Lista"     to MaterialTheme.colorScheme.tertiary
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = PILL_RADIUS
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FocusNoteItem(
    note: Note,
    isFocused: Boolean,
    canFocus: Boolean,
    onToggle: () -> Unit
) {
    val bgColor = if (isFocused)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.surfaceContainer

    val accentColor = when (note.category) {
        SessionType.WORK         -> COLOR_WORK
        SessionType.STUDY        -> COLOR_STUDY
        SessionType.UNIDENTIFIED -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = ITEM_RADIUS,
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dot de categoría
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Column {
                    Text(note.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    val catLabel = when (note.category) {
                        SessionType.WORK         -> "Trabajo"
                        SessionType.STUDY        -> "Estudio"
                        SessionType.UNIDENTIFIED -> "Sin categoría"
                    }
                    Text(
                        if (note.tasks.isNotEmpty()) {
                            val done = note.tasks.count { it.isDone }
                            "$catLabel · $done/${note.tasks.size} tareas"
                        } else catLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onToggle, enabled = canFocus) {
                Icon(
                    imageVector = if (isFocused) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (isFocused) "Desactivar foco" else "Activar",
                    tint = when {
                        isFocused  -> MaterialTheme.colorScheme.primary
                        canFocus   -> MaterialTheme.colorScheme.onSurfaceVariant
                        else       -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(
    label: String,
    minutes: Int,
    color: Color,
    ratio: Float? = null,
    overrideLabel: String? = null
) {
    val displayRatio = ratio ?: (minutes.toFloat() / 240f).coerceIn(0f, 1f)
    val displayRight = overrideLabel ?: minutes.asHm()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(displayRight, style = MaterialTheme.typography.labelMedium, color = color)
        }
        LinearProgressIndicator(
            progress = { displayRatio },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(PILL_RADIUS),
            color = color,
            trackColor = color.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun TodayNoteItem(note: Note) {
    val (bg, accent) = when (note.status) {
        NoteStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) to MaterialTheme.colorScheme.tertiary
        NoteStatus.ACTIVE    -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) to MaterialTheme.colorScheme.primary
        NoteStatus.PENDING   -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.outline
    }
    val catLabel = when (note.category) {
        SessionType.WORK         -> "Trabajo"
        SessionType.STUDY        -> "Estudio"
        SessionType.UNIDENTIFIED -> "Sin categoría"
    }
    val statusLabel = when (note.status) {
        NoteStatus.PENDING   -> "Pendiente"
        NoteStatus.ACTIVE    -> "Activa"
        NoteStatus.COMPLETED -> "Completada"
    }
    Surface(color = bg, shape = ITEM_RADIUS, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
                Column {
                    Text(note.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Text("$catLabel · $statusLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (note.status == NoteStatus.COMPLETED) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Navigation bar compartida ─────────────────────────────────────────────────

@Composable
fun AppBottomBar(
    selected: Int,
    onNotes: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onPlanner: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selected == 0, onClick = {},
            icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = selected == 1, onClick = onNotes,
            icon = { Icon(Icons.Filled.EventNote, null) }, label = { Text("Notas") }
        )
        NavigationBarItem(
            selected = selected == 2, onClick = onNotifications,
            icon = { Icon(Icons.Filled.Notifications, null) }, label = { Text("Alertas") }
        )
        NavigationBarItem(
            selected = selected == 3, onClick = onPlanner,
            icon = { Icon(Icons.Filled.EventNote, null) }, label = { Text("Semana") }
        )
    }
}
