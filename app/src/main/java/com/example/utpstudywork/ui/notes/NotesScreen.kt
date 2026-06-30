package com.example.utpstudywork.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.utpstudywork.core.ClassificationResult
import com.example.utpstudywork.core.ClassificationSource
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.SessionType
import com.example.utpstudywork.domain.model.Task

// ── Tokens de diseño ──────────────────────────────────────────────────────────

private val CARD_RADIUS = RoundedCornerShape(20.dp)
private val PILL        = RoundedCornerShape(50.dp)
private val FIELD_SHAPE = RoundedCornerShape(16.dp)

private fun categoryColor(category: SessionType, scheme: ColorScheme): Color = when (category) {
    SessionType.WORK         -> Color(0xFF4F7CF6)
    SessionType.STUDY        -> Color(0xFF7C5DF6)
    SessionType.UNIDENTIFIED -> scheme.outline
}

private fun categoryLabel(category: SessionType) = when (category) {
    SessionType.WORK         -> "Trabajo"
    SessionType.STUDY        -> "Estudio"
    SessionType.UNIDENTIFIED -> "Sin categoría"
}

// ── Pantalla ─────────────────────────────────────────────────────────────────

@Composable
fun NotesScreen(vm: NotesViewModel, onBackClick: () -> Unit) {
    val state = vm.state.collectAsState().value
    NotesContent(
        state = state,
        onBackClick = onBackClick,
        onSelectTab = vm::selectTab,
        onToggleShowCompleted = vm::toggleShowCompleted,
        onSearchChange = vm::updateSearch,
        onEditNote = vm::openDialog,
        onDeleteNote = vm::deleteNote,
        onOpenDialog = vm::openDialog,
        onCloseDialog = vm::closeDialog,
        onTitleChange = vm::updateTitle,
        onDescriptionChange = vm::updateDescription,
        onCategoryChange = vm::updateCategory,
        onColorChange = vm::updateColor,
        onSaveNote = vm::saveNote,
        onNewTaskTextChange = vm::updateNewTaskText,
        onAddTask = { vm.addTaskToNote(state.editingNote?.id ?: return@NotesContent) },
        onToggleTask = vm::toggleTaskDone,
        onDeleteTask = vm::deleteTask,
        onSetStatus = vm::setStatus,
        onAcceptSuggestion = vm::acceptSuggestion,
        onDismissSuggestion = vm::dismissSuggestion
    )
}

@Composable
fun NotesContent(
    state: NotesUiState,
    onBackClick: () -> Unit,
    onSelectTab: (SessionType?) -> Unit,
    onToggleShowCompleted: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onOpenDialog: (Note?) -> Unit,
    onCloseDialog: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (SessionType) -> Unit,
    onColorChange: (Int) -> Unit,
    onSaveNote: () -> Unit,
    onNewTaskTextChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSetStatus: (String, NoteStatus) -> Unit,
    onAcceptSuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenDialog(null) },
                shape = PILL,
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                    Text("Nueva nota", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mis Notas", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "${state.filteredNotes.size} nota${if (state.filteredNotes.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onBackClick, shape = PILL) {
                    Icon(Icons.Filled.Close, "Cerrar", modifier = Modifier.size(18.dp))
                }
            }

            // ── Barra de búsqueda pill ────────────────────────────────────────
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar notas…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = PILL,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outlineVariant,
                    focusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Spacer(Modifier.height(12.dp))

            // ── Filtros ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.showCompleted) {
                    listOf<SessionType?>(null, SessionType.WORK, SessionType.STUDY, SessionType.UNIDENTIFIED).forEach { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            label = {
                                Text(
                                    when (tab) {
                                        null                     -> "Todas"
                                        SessionType.WORK         -> "Trabajo"
                                        SessionType.STUDY        -> "Estudio"
                                        SessionType.UNIDENTIFIED -> "Sin cat."
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = PILL
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Toggle completadas
                FilterChip(
                    selected = state.showCompleted,
                    onClick = onToggleShowCompleted,
                    label = {
                        Text(
                            if (state.showCompleted) "Activas" else "✓ ${state.completedNotes.size}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    shape = PILL
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Lista de notas ────────────────────────────────────────────────
            val displayedNotes = if (state.showCompleted) state.completedNotes else state.filteredNotes

            if (displayedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.EventNote,
                            null,
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            if (state.showCompleted) "Sin notas completadas" else "Sin notas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedNotes) { note ->
                        NoteCard(
                            note = note,
                            onEdit = { onEditNote(note) },
                            onDelete = { onDeleteNote(note.id) },
                            onToggleTask = onToggleTask,
                            onDeleteTask = onDeleteTask,
                            onSetStatus = { status -> onSetStatus(note.id, status) }
                        )
                    }
                }
            }
        }
    }

    // ── Diálogo de creación/edición ───────────────────────────────────────────
    if (state.showDialog) {
        val editingNote = state.editingNote
        AlertDialog(
            onDismissRequest = onCloseDialog,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    if (editingNote != null) "Editar nota" else "Nueva nota",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.newTitle,
                        onValueChange = onTitleChange,
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = FIELD_SHAPE
                    )
                    OutlinedTextField(
                        value = state.newDescription,
                        onValueChange = onDescriptionChange,
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        maxLines = 4,
                        shape = FIELD_SHAPE
                    )

                    // Sugerencia de clasificador
                    val suggestion = state.suggestion
                    AnimatedVisibility(
                        visible = suggestion != null && !state.suggestionDismissed,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        if (suggestion != null) {
                            ClassifierBanner(
                                result = suggestion,
                                onAccept = onAcceptSuggestion,
                                onDismiss = onDismissSuggestion
                            )
                        }
                    }

                    // Categoría
                    Text("Categoría", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(SessionType.WORK, SessionType.STUDY, SessionType.UNIDENTIFIED).forEach { cat ->
                            FilterChip(
                                selected = state.newCategory == cat,
                                onClick = { onCategoryChange(cat) },
                                label = {
                                    Text(
                                        when (cat) {
                                            SessionType.WORK         -> "Trabajo"
                                            SessionType.STUDY        -> "Estudio"
                                            SessionType.UNIDENTIFIED -> "Sin cat."
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                shape = PILL,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Color
                    Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val palette = listOf(
                        0xFFBBDEFB.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(),
                        0xFFFFCCBC.toInt(), 0xFFE1BEE7.toInt()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (c in palette) {
                            val selected = state.newColor == c
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                    .clickable { onColorChange(c) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Tareas (solo al editar)
                    if (editingNote != null) {
                        HorizontalDivider()
                        Text("Tareas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        editingNote.tasks.forEach { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = task.isDone,
                                    onCheckedChange = { onToggleTask(task) }
                                )
                                Text(
                                    task.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                                    ),
                                    modifier = Modifier.weight(1f),
                                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { onDeleteTask(task.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.newTaskText,
                                onValueChange = onNewTaskTextChange,
                                placeholder = { Text("Nueva tarea…") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = FIELD_SHAPE
                            )
                            FilledTonalIconButton(onClick = onAddTask, shape = CircleShape) {
                                Icon(Icons.Filled.Add, "Agregar tarea")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onSaveNote, shape = PILL) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = onCloseDialog) { Text("Cancelar") }
            }
        )
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun ClassifierBanner(
    result: ClassificationResult,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val categoryLabel = if (result.suggested == SessionType.WORK) "Trabajo" else "Estudio"
    val confidencePct = (result.confidence * 100).toInt()
    val sourceLabel = when (result.source) {
        ClassificationSource.LEARNED  -> " · aprendido"
        ClassificationSource.COMBINED -> " · combinado"
        else                          -> ""
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sugerido: $categoryLabel ($confidencePct%$sourceLabel)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onAccept, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Aplicar", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSetStatus: (NoteStatus) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val catColor = categoryColor(note.category, scheme)
    val isCompleted = note.status == NoteStatus.COMPLETED

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_RADIUS,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(note.color).copy(alpha = if (isCompleted) 0.5f else 0.85f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra izquierda de categoría
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(catColor, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            )
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            note.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                            )
                        )
                        if (note.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                note.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        // Badge de categoría
                        Surface(
                            color = catColor.copy(alpha = 0.12f),
                            shape = PILL
                        ) {
                            Text(
                                categoryLabel(note.category),
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp), tint = scheme.error.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Estado
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoteStatus.entries.forEach { s ->
                        val label = when (s) {
                            NoteStatus.PENDING   -> "Pendiente"
                            NoteStatus.ACTIVE    -> "Activa"
                            NoteStatus.COMPLETED -> "Lista"
                        }
                        FilterChip(
                            selected = note.status == s,
                            onClick = { onSetStatus(s) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = PILL
                        )
                    }
                }

                // Tareas
                if (note.tasks.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val done = note.tasks.count { it.isDone }
                    LinearProgressIndicator(
                        progress = { done.toFloat() / note.tasks.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(PILL),
                        color = catColor,
                        trackColor = catColor.copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$done de ${note.tasks.size} tareas completadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    note.tasks.forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { onToggleTask(task) },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                task.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                                ),
                                modifier = Modifier.weight(1f),
                                color = if (task.isDone) scheme.onSurfaceVariant else scheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
