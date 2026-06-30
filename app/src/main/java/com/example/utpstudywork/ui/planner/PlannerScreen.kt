package com.example.utpstudywork.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.Objective
import com.example.utpstudywork.domain.model.SessionType
import java.time.LocalDate

// ── Constantes de layout ──────────────────────────────────────────────────────

private val LABEL_COL_WIDTH = 130.dp
private val DAY_COL_WIDTH   = 140.dp
private val ROW_MIN_HEIGHT  = 80.dp

// ── Paleta de objetivos ───────────────────────────────────────────────────────

private val OBJECTIVE_COLORS = listOf(
    Color(0xFF5C6BC0) to Color(0xFFE8EAF6),
    Color(0xFF26A69A) to Color(0xFFE0F2F1),
    Color(0xFFEF7C00) to Color(0xFFFFF3E0),
    Color(0xFFE53935) to Color(0xFFFFEBEE),
    Color(0xFF8E24AA) to Color(0xFFF3E5F5),
    Color(0xFF43A047) to Color(0xFFE8F5E9),
)

private fun objectiveColor(index: Int) = OBJECTIVE_COLORS[index % OBJECTIVE_COLORS.size]

// ── Helpers de categoría ──────────────────────────────────────────────────────

private fun categoryLabel(type: SessionType?) = when (type) {
    SessionType.WORK         -> "Trabajo"
    SessionType.STUDY        -> "Estudio"
    SessionType.UNIDENTIFIED -> "Sin categoría"
    null                     -> "Todas"
}

// ── Pantalla principal ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(vm: PlannerViewModel, onBackClick: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val vertScroll  = rememberScrollState()

    val isCurrentWeek = st.weekId == PlannerViewModel.weekIdFor(PlannerViewModel.currentWeekStartMillis())
    val categoryFilter = st.categoryFilter

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(st.weekLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = { vm.goToPreviousWeek() }) { Icon(Icons.Default.ChevronLeft, "Semana anterior") }
                    IconButton(onClick = { vm.goToCurrentWeek() }) {
                        Text("Hoy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { vm.goToNextWeek() }) { Icon(Icons.Default.ChevronRight, "Semana siguiente") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openNewObjectiveDialog() }) {
                Icon(Icons.Default.Add, "Nuevo objetivo")
            }
        }
    ) { padding ->

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Banner de semana actual ────────────────────────────────────────
            if (isCurrentWeek) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "i  Las notas asignadas al día de hoy se activarán automáticamente y recibirán una notificación",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // ── Filtros de categoría ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // null = sin filtro (todas las categorías)
                listOf<SessionType?>(null, SessionType.WORK, SessionType.STUDY, SessionType.UNIDENTIFIED).forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { vm.setCategoryFilter(if (categoryFilter == cat) null else cat) },
                        label = { Text(categoryLabel(cat), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // ── Cabecera fija de días ─────────────────────────────────────────
            TableHeader(scrollState)
            HorizontalDivider()

            // ── Cuerpo scrollable vertical ────────────────────────────────────
            Column(modifier = Modifier.weight(1f).verticalScroll(vertScroll)) {

                if (st.objectives.isEmpty() && st.looseNotes.isEmpty()) {
                    EmptyPlanner { vm.openNewObjectiveDialog() }
                } else {
                    // Fila por objetivo
                    st.objectives.forEachIndexed { idx, obj ->
                        val (accent, bg) = objectiveColor(idx)
                        ObjectiveRow(
                            objective = obj,
                            accentColor = accent,
                            bgColor = bg,
                            scrollState = scrollState,
                            categoryFilter = categoryFilter,
                            onAddNote = { day -> vm.openAssignNoteDialog(day, obj.id) },
                            onToggleNote = vm::toggleNoteDone,
                            onRemoveNote = { note -> vm.removeNoteFromCalendar(note.id) },
                            onEditObjective = { vm.openNewObjectiveDialog(obj) },
                            onDeleteObjective = { vm.deleteObjective(obj.id) },
                            onToggleDone = { vm.toggleObjectiveDone(obj) }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }

                    // Fila de notas sueltas
                    LooseNotesRow(
                        looseNotes = st.looseNotes,
                        scrollState = scrollState,
                        categoryFilter = categoryFilter,
                        onAddNote = { day -> vm.openAssignNoteDialog(day, null) },
                        onToggleNote = vm::toggleNoteDone,
                        onRemoveNote = { note -> vm.removeNoteFromCalendar(note.id) },
                        onAssignToObjective = vm::openAssignToObjectiveDialog
                    )
                }
            }
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────
    when (val d = st.dialog) {
        is PlannerDialog.NewObjective      -> NewObjectiveDialog(d, vm)
        is PlannerDialog.AssignNote        -> AssignNoteDialog(d, st.unassignedNotes, vm)
        is PlannerDialog.CreateAndAssign   -> CreateAndAssignDialog(d, vm)
        is PlannerDialog.AssignToObjective -> AssignToObjectiveDialog(d, st.objectives, vm)
        else -> {}
    }
}

// ── Cabecera de la tabla ──────────────────────────────────────────────────────

@Composable
private fun TableHeader(scrollState: androidx.compose.foundation.ScrollState) {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(modifier = Modifier.width(LABEL_COL_WIDTH).padding(8.dp))
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            PlannerViewModel.DAY_LABELS.forEach { label ->
                Box(
                    modifier = Modifier.width(DAY_COL_WIDTH).padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Fila de objetivo ──────────────────────────────────────────────────────────

@Composable
private fun ObjectiveRow(
    objective: Objective,
    accentColor: Color,
    bgColor: Color,
    scrollState: androidx.compose.foundation.ScrollState,
    categoryFilter: SessionType?,
    onAddNote: (Int) -> Unit,
    onToggleNote: (Note) -> Unit,
    onRemoveNote: (Note) -> Unit,
    onEditObjective: () -> Unit,
    onDeleteObjective: () -> Unit,
    onToggleDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = ROW_MIN_HEIGHT),
        verticalAlignment = Alignment.Top
    ) {
        ObjectiveLabelCell(
            objective = objective,
            accentColor = accentColor,
            bgColor = bgColor,
            onEdit = onEditObjective,
            onDelete = onDeleteObjective,
            onToggleDone = onToggleDone
        )
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            for (day in 1..7) {
                val dayNotes = objective.notes
                    .filter { it.calendarDayOfWeek == day }
                    .filter { categoryFilter == null || it.category == categoryFilter }
                DayNoteCell(
                    notes = dayNotes,
                    cellBg = bgColor.copy(alpha = 0.25f),
                    onAddClick = { onAddNote(day) },
                    onToggle = onToggleNote,
                    onRemove = onRemoveNote
                )
            }
        }
    }
}

@Composable
private fun ObjectiveLabelCell(
    objective: Objective,
    accentColor: Color,
    bgColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(LABEL_COL_WIDTH)
            .heightIn(min = ROW_MIN_HEIGHT)
            .background(bgColor.copy(alpha = 0.4f))
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = objective.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (objective.isDone) TextDecoration.LineThrough else null
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        val done = objective.notes.count { it.status == NoteStatus.COMPLETED }
        val total = objective.notes.size
        if (total > 0) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accentColor
            )
            Text("$done/$total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }

        Row {
            IconButton(onClick = onToggleDone, modifier = Modifier.size(20.dp)) {
                Text(if (objective.isDone) "✓" else "○", fontSize = 11.sp, color = accentColor)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Celda de día con notas ────────────────────────────────────────────────────

@Composable
private fun DayNoteCell(
    notes: List<Note>,
    cellBg: Color,
    onAddClick: () -> Unit,
    onToggle: (Note) -> Unit,
    onRemove: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .width(DAY_COL_WIDTH)
            .heightIn(min = ROW_MIN_HEIGHT)
            .background(cellBg)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        notes.forEach { note ->
            NoteChip(note = note, onToggle = { onToggle(note) }, onRemove = { onRemove(note) })
            Spacer(Modifier.height(2.dp))
        }
        TextButton(
            onClick = onAddClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text("Agregar", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Fila de notas sueltas ─────────────────────────────────────────────────────

@Composable
private fun LooseNotesRow(
    looseNotes: List<Note>,
    scrollState: androidx.compose.foundation.ScrollState,
    categoryFilter: SessionType?,
    onAddNote: (Int) -> Unit,
    onToggleNote: (Note) -> Unit,
    onRemoveNote: (Note) -> Unit,
    onAssignToObjective: (Note) -> Unit
) {
    val looseBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = ROW_MIN_HEIGHT),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(LABEL_COL_WIDTH)
                .heightIn(min = ROW_MIN_HEIGHT)
                .background(looseBg)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Notas sueltas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            Text("Sin objetivo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            for (day in 1..7) {
                val dayNotes = looseNotes
                    .filter { it.calendarDayOfWeek == day }
                    .filter { categoryFilter == null || it.category == categoryFilter }
                Column(
                    modifier = Modifier
                        .width(DAY_COL_WIDTH)
                        .heightIn(min = ROW_MIN_HEIGHT)
                        .background(looseBg)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    dayNotes.forEach { note ->
                        LooseNoteChip(
                            note = note,
                            onToggle = { onToggleNote(note) },
                            onRemove = { onRemoveNote(note) },
                            onAssign = { onAssignToObjective(note) }
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    TextButton(
                        onClick = { onAddNote(day) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Suelta", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ── Chips de nota ─────────────────────────────────────────────────────────────

@Composable
private fun NoteChip(note: Note, onToggle: () -> Unit, onRemove: () -> Unit) {
    val isDone = note.status == NoteStatus.COMPLETED
    val catLabel = when (note.category) {
        SessionType.WORK         -> "W"
        SessionType.STUDY        -> "S"
        SessionType.UNIDENTIFIED -> "?"
    }
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(6.dp),
        color = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            // Etiqueta de categoría pequeña
            Text(
                catLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = when (note.category) {
                    SessionType.WORK         -> MaterialTheme.colorScheme.primary
                    SessionType.STUDY        -> MaterialTheme.colorScheme.tertiary
                    SessionType.UNIDENTIFIED -> MaterialTheme.colorScheme.outline
                },
                modifier = Modifier
                    .background(
                        color = when (note.category) {
                            SessionType.WORK         -> MaterialTheme.colorScheme.primaryContainer
                            SessionType.STUDY        -> MaterialTheme.colorScheme.tertiaryContainer
                            SessionType.UNIDENTIFIED -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(4.dp))
            if (isDone) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = note.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LooseNoteChip(note: Note, onToggle: () -> Unit, onRemove: () -> Unit, onAssign: () -> Unit) {
    val isDone = note.status == NoteStatus.COMPLETED
    val catLabel = when (note.category) {
        SessionType.WORK         -> "W"
        SessionType.STUDY        -> "S"
        SessionType.UNIDENTIFIED -> "?"
    }
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(6.dp),
        color = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                catLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(4.dp))
            if (isDone) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = note.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAssign, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Link, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Placeholder vacío ─────────────────────────────────────────────────────────

@Composable
private fun EmptyPlanner(onNewObjective: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EventNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text("Sin objetivos esta semana", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Crea un objetivo para organizar tus notas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNewObjective) { Text("Nuevo objetivo") }
    }
}

// ── Diálogos ─────────────────────────────────────────────────────────────────

@Composable
private fun NewObjectiveDialog(d: PlannerDialog.NewObjective, vm: PlannerViewModel) {
    AlertDialog(
        onDismissRequest = vm::closeDialog,
        title = { Text(if (d.editingId != null) "Editar objetivo" else "Nuevo objetivo") },
        text = {
            OutlinedTextField(
                value = d.title,
                onValueChange = vm::updateObjectiveDialogTitle,
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = vm::saveObjective, enabled = d.title.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = vm::closeDialog) { Text("Cancelar") } }
    )
}

@Composable
private fun AssignNoteDialog(
    d: PlannerDialog.AssignNote,
    unassignedNotes: List<Note>,
    vm: PlannerViewModel
) {
    val dayLabel = PlannerViewModel.DAY_LABELS.getOrElse(d.dayOfWeek - 1) { "Día ${d.dayOfWeek}" }

    AlertDialog(
        onDismissRequest = vm::closeDialog,
        title = {
            Column {
                Text("Asignar nota al calendario")
                Text(dayLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { vm.openCreateNoteDialog(d.dayOfWeek, d.objectiveId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Crear nueva nota")
                }

                if (unassignedNotes.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Notas existentes sin asignar:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    unassignedNotes.forEach { note ->
                        Surface(
                            onClick = { vm.assignNoteToDay(note, d.dayOfWeek, d.objectiveId) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(note.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    CategoryBadge(note.category)
                                }
                                if (note.description.isNotBlank()) {
                                    Text(
                                        note.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No hay notas sin asignar. Crea una nueva.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = vm::closeDialog) { Text("Cancelar") } }
    )
}

@Composable
private fun CategoryBadge(category: SessionType) {
    val (label, color) = when (category) {
        SessionType.WORK         -> "Trabajo"     to MaterialTheme.colorScheme.primary
        SessionType.STUDY        -> "Estudio"     to MaterialTheme.colorScheme.tertiary
        SessionType.UNIDENTIFIED -> "Sin categoría" to MaterialTheme.colorScheme.outline
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CreateAndAssignDialog(d: PlannerDialog.CreateAndAssign, vm: PlannerViewModel) {
    val dayLabel = PlannerViewModel.DAY_LABELS.getOrElse(d.dayOfWeek - 1) { "Día ${d.dayOfWeek}" }
    val categories = listOf(SessionType.WORK, SessionType.STUDY, SessionType.UNIDENTIFIED)

    AlertDialog(
        onDismissRequest = vm::closeDialog,
        title = {
            Column {
                Text("Nueva nota")
                Text(dayLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = d.title,
                    onValueChange = vm::updateCreateNoteTitle,
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = d.description,
                    onValueChange = vm::updateCreateNoteDescription,
                    label = { Text("Descripción (opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Selector de categoría ───────────────────────────────────
                Text(
                    "Categoría",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = d.category == cat,
                            onClick = { vm.updateCreateNoteCategory(cat) },
                            label = {
                                Text(
                                    when (cat) {
                                        SessionType.WORK         -> "Trabajo"
                                        SessionType.STUDY        -> "Estudio"
                                        SessionType.UNIDENTIFIED -> "Sin categoría"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = vm::createAndAssignNote, enabled = d.title.isNotBlank()) {
                Text("Crear y asignar")
            }
        },
        dismissButton = { TextButton(onClick = vm::closeDialog) { Text("Cancelar") } }
    )
}

@Composable
private fun AssignToObjectiveDialog(
    d: PlannerDialog.AssignToObjective,
    objectives: List<Objective>,
    vm: PlannerViewModel
) {
    AlertDialog(
        onDismissRequest = vm::closeDialog,
        title = {
            Column {
                Text("Asignar a objetivo")
                Text(d.note.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        },
        text = {
            if (objectives.isEmpty()) {
                Text("No hay objetivos esta semana. Crea uno primero.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    objectives.forEachIndexed { idx, obj ->
                        val (accent, bg) = objectiveColor(idx)
                        Surface(
                            onClick = { vm.assignNoteToObjective(d.note, obj) },
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
                                Spacer(Modifier.width(8.dp))
                                Text(obj.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = vm::closeDialog) { Text("Cancelar") } }
    )
}
