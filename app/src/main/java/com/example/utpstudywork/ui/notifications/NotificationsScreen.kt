package com.example.utpstudywork.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NotificationType
import com.example.utpstudywork.domain.model.SessionType

private val CARD_RADIUS = RoundedCornerShape(20.dp)
private val PILL        = RoundedCornerShape(50.dp)
private val FIELD_SHAPE = RoundedCornerShape(14.dp)

@Composable
fun NotificationsScreen(vm: NotificationsViewModel, onBackClick: () -> Unit) {
    val state = vm.state.collectAsState().value
    NotificationsContent(
        state = state,
        onBackClick = onBackClick,
        onToggleExpand = vm::toggleExpand,
        onSetNone = vm::setNoNotification,
        onSetDefault = vm::setDefaultNotification,
        onSetCustom = vm::setCustomNotification
    )
}

@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    onBackClick: () -> Unit,
    onToggleExpand: (String) -> Unit,
    onSetNone: (String) -> Unit,
    onSetDefault: (String) -> Unit,
    onSetCustom: (String, String, SessionType?) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Scaffold(containerColor = scheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalIconButton(onClick = onBackClick, shape = PILL) {
                    Icon(Icons.Filled.ArrowBack, "Volver", modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        "Notificaciones",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Configura alertas por nota",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            if (state.notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.NotificationsOff,
                            null,
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            "No hay notas creadas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.notes) { note ->
                        NoteNotificationCard(
                            note = note,
                            isExpanded = state.expandedNoteId == note.id,
                            onToggleExpand = { onToggleExpand(note.id) },
                            onSetNone = { onSetNone(note.id) },
                            onSetDefault = { onSetDefault(note.id) },
                            onSetCustom = { msg, cat -> onSetCustom(note.id, msg, cat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteNotificationCard(
    note: Note,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSetNone: () -> Unit,
    onSetDefault: () -> Unit,
    onSetCustom: (String, SessionType?) -> Unit
) {
    var customMessage by remember(note.id, note.notification) {
        mutableStateOf(note.notification?.customMessage ?: "")
    }
    var customFilter by remember(note.id, note.notification) {
        mutableStateOf(note.notification?.filterCategory)
    }

    val currentType = note.notification?.type
    val (statusLabel, statusColor) = when (currentType) {
        null                     -> "Sin notificación"  to MaterialTheme.colorScheme.outline
        NotificationType.DEFAULT -> "Predeterminada"    to MaterialTheme.colorScheme.primary
        NotificationType.CUSTOM  -> "Personalizada"     to MaterialTheme.colorScheme.tertiary
    }

    val catColor = when (note.category) {
        SessionType.WORK         -> Color(0xFF4F7CF6)
        SessionType.STUDY        -> Color(0xFF7C5DF6)
        SessionType.UNIDENTIFIED -> MaterialTheme.colorScheme.outline
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_RADIUS,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(note.color).copy(alpha = 0.8f))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra lateral de categoría
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(catColor, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            )
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Dot de categoría
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(catColor))
                        Column {
                            Text(
                                note.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                when (note.category) {
                                    SessionType.WORK         -> "Trabajo"
                                    SessionType.STUDY        -> "Estudio"
                                    SessionType.UNIDENTIFIED -> "Sin categoría"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Badge de estado
                        Surface(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = PILL
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (currentType != null) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                    null,
                                    tint = statusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
                            }
                        }

                        IconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ── Panel expandible ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(14.dp))

                        Text(
                            "Tipo de notificación",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = currentType == null,
                                onClick = onSetNone,
                                label = { Text("Ninguna", style = MaterialTheme.typography.labelSmall) },
                                shape = PILL,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = currentType == NotificationType.DEFAULT,
                                onClick = onSetDefault,
                                label = { Text("Predeter.", style = MaterialTheme.typography.labelSmall) },
                                shape = PILL,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = currentType == NotificationType.CUSTOM,
                                onClick = { onSetCustom(customMessage, customFilter) },
                                label = { Text("Personaliz.", style = MaterialTheme.typography.labelSmall) },
                                shape = PILL,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Descripción del tipo
                        val description = when (currentType) {
                            null                     -> "La nota no enviará notificaciones."
                            NotificationType.DEFAULT -> "Se notifica cuando inicia el Pomodoro de esta categoría."
                            NotificationType.CUSTOM  -> "Notificación con mensaje y filtro personalizados."
                        }
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Opciones CUSTOM
                        AnimatedVisibility(
                            visible = currentType == NotificationType.CUSTOM,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = customMessage,
                                    onValueChange = { customMessage = it },
                                    label = { Text("Mensaje personalizado") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = FIELD_SHAPE,
                                    trailingIcon = {
                                        TextButton(onClick = { onSetCustom(customMessage, customFilter) }) {
                                            Text("Guardar", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                )

                                Text(
                                    "Activar solo durante:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = customFilter == null,
                                        onClick = { customFilter = null; onSetCustom(customMessage, null) },
                                        label = { Text("Cualquier", style = MaterialTheme.typography.labelSmall) },
                                        shape = PILL,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = customFilter == SessionType.WORK,
                                        onClick = { customFilter = SessionType.WORK; onSetCustom(customMessage, SessionType.WORK) },
                                        label = { Text("Trabajo", style = MaterialTheme.typography.labelSmall) },
                                        shape = PILL,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = customFilter == SessionType.STUDY,
                                        onClick = { customFilter = SessionType.STUDY; onSetCustom(customMessage, SessionType.STUDY) },
                                        label = { Text("Estudio", style = MaterialTheme.typography.labelSmall) },
                                        shape = PILL,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
