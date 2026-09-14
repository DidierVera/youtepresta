package com.didiprogrammer.youtepresta.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.data.model.Friend
import com.didiprogrammer.youtepresta.ui.common.DialogButtonRow
import com.didiprogrammer.youtepresta.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadFriends() }

    val uiState by viewModel.uiState.collectAsState()
    val isDialogVisible by viewModel.isDialogVisible.collectAsState()
    val friendPendingDelete by viewModel.friendPendingDelete.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Amigos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear amigo")
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FriendsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FriendsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Button(onClick = { viewModel.loadFriends() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is FriendsUiState.Content -> {
                if (state.friends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no tienes amigos. Crea el primero con el botón +.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(Spacing.md)
                    ) {
                        items(state.friends, key = { it.id }) { friend ->
                            FriendRow(
                                friend = friend,
                                onEdit = { viewModel.showEditDialog(friend) },
                                onDelete = { viewModel.confirmDelete(friend) }
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                    }
                }
            }
        }
    }

    if (isDialogVisible) {
        FriendDialog(viewModel = viewModel)
    }

    if (friendPendingDelete != null) {
        DeleteFriendDialog(viewModel = viewModel, friend = friendPendingDelete!!)
    }
}

@Composable
private fun FriendRow(friend: Friend, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = friend.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!friend.phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(text = friend.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar amigo")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar amigo")
                }
            }
        }
    }
}

@Composable
private fun FriendDialog(viewModel: FriendsViewModel) {
    val isSaving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val editingFriend by viewModel.editingFriend.collectAsState()

    var name by remember(editingFriend) { mutableStateOf(editingFriend?.name.orEmpty()) }
    var phone by remember(editingFriend) { mutableStateOf(editingFriend?.phone.orEmpty()) }
    var notes by remember(editingFriend) { mutableStateOf(editingFriend?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
        title = { Text(if (editingFriend != null) "Editar amigo" else "Nuevo amigo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono (opcional)") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                if (saveError != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = saveError.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            DialogButtonRow(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.saveFriend(name, phone, notes) },
                confirmText = if (editingFriend != null) "Guardar" else "Crear",
                enabled = !isSaving,
                isLoading = isSaving
            )
        }
    )
}

@Composable
private fun DeleteFriendDialog(viewModel: FriendsViewModel, friend: Friend) {
    val isDeleting by viewModel.isDeleting.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.dismissDeleteConfirmation() },
        title = { Text("Eliminar amigo") },
        text = {
            Column {
                Text("¿Seguro que quieres eliminar a ${friend.name}? Esta acción no se puede deshacer.")
                if (deleteError != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(text = deleteError.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            DialogButtonRow(
                onDismiss = { viewModel.dismissDeleteConfirmation() },
                onConfirm = { viewModel.deleteFriend() },
                confirmText = "Eliminar",
                enabled = !isDeleting,
                isLoading = isDeleting,
                confirmColor = MaterialTheme.colorScheme.error
            )
        }
    )
}
