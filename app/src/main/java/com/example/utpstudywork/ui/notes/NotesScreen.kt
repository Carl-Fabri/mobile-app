package com.example.utpstudywork.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.SessionType

@Composable
fun NotesScreen(vm: NotesViewModel, onBackClick: () -> Unit) {
    val state = vm.state.collectAsState().value
    NotesContent(
        state = state,
        onBackClick = onBackClick,
        onSelectTab = vm::selectTab,
        onAddNote = { vm.openDialog() },
        onEditNote = vm::openDialog,
        onDeleteNote = vm::deleteNote,
        onOpenDialog = vm::openDialog,
        onCloseDialog = vm::closeDialog,
        onTitleChange = vm::updateTitle,
        onDescriptionChange = vm::updateDescription,
        onCategoryChange = vm::updateCategory,
        onColorChange = vm::updateColor,
        onSaveNote = vm::saveNote
    )
}

@Composable
fun NotesContent(
    state: NotesUiState,
    onBackClick: () -> Unit,
    onSelectTab: (SessionType?) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onOpenDialog: (Note?) -> Unit,
    onCloseDialog: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (SessionType) -> Unit,
    onColorChange: (Int) -> Unit,
    onSaveNote: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mis Notas", style = MaterialTheme.typography.headlineSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { onOpenDialog(null) },
                        modifier = Modifier.size(40.dp),
                        containerColor = primary
                    ) {
                        Icon(Icons.Filled.Add, "Agregar", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.Close, "Cerrar")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search bar
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Buscar notas...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                }
            )

            // Tabs
            TabRow(
                selectedTabIndex = when (state.selectedTab) {
                    null -> 0
                    SessionType.WORK -> 1
                    SessionType.STUDY -> 2
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = state.selectedTab == null,
                    onClick = { onSelectTab(null) },
                    text = { Text("Todas") }
                )
                Tab(
                    selected = state.selectedTab == SessionType.WORK,
                    onClick = { onSelectTab(SessionType.WORK) },
                    text = { Text("Trabajo") }
                )
                Tab(
                    selected = state.selectedTab == SessionType.STUDY,
                    onClick = { onSelectTab(SessionType.STUDY) },
                    text = { Text("Estudio") }
                )
            }

            // Notes list - filter according to selected tab
            val filteredNotes = if (state.selectedTab == null) {
                state.allNotes
            } else {
                state.allNotes.filter { it.category == state.selectedTab }
            }

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin notas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotes) { note ->
                        NoteCard(
                            note = note,
                            onEdit = { onEditNote(note) },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialog para crear/editar notas
    if (state.showDialog) {
        AlertDialog(
            onDismissRequest = onCloseDialog,
            title = {
                Text(
                    if (state.editingNote != null) "Editar Nota" else "Nueva Nota"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = state.newTitle,
                        onValueChange = onTitleChange,
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    TextField(
                        value = state.newDescription,
                        onValueChange = onDescriptionChange,
                        label = { Text("Descripción") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 5
                    )
                    Text("Categoría", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.newCategory == SessionType.WORK,
                            onClick = { onCategoryChange(SessionType.WORK) },
                            label = { Text("Trabajo") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.newCategory == SessionType.STUDY,
                            onClick = { onCategoryChange(SessionType.STUDY) },
                            label = { Text("Estudio") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Color", style = MaterialTheme.typography.labelMedium)
                    val palette = listOf(
                        0xFFBBDEFB.toInt(), // light blue
                        0xFFC8E6C9.toInt(), // light green
                        0xFFFFF9C4.toInt(), // light yellow
                        0xFFFFCCBC.toInt(), // light orange
                        0xFFE1BEE7.toInt()  // light purple
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in palette) {
                            val selected = state.newColor == c
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier),
                                color = Color(c)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().clickable { onColorChange(c) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onSaveNote) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(note.color)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (note.category == SessionType.WORK) "Trabajo" else "Estudio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, "Editar", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "Eliminar", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
