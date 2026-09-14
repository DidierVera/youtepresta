package com.didiprogrammer.youtepresta.ui.friends

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.data.model.Friend

/**
 * Reusable "search or create friend" component. Loads the friend list once and filters it in
 * memory as the user types; if nothing matches, offers to create a new friend inline.
 * Intended to be embedded inside other forms (e.g. the new loan form in Phase 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendPicker(
    onFriendSelected: (Friend) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Buscar o crear amigo",
    viewModel: FriendPickerViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadFriends() }

    val query by viewModel.query.collectAsState()
    val filteredFriends by viewModel.filteredFriends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val error by viewModel.error.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && query.isNotBlank() && !isLoading,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    viewModel.onQueryChange(it)
                    expanded = true
                },
                label = { Text(label) },
                singleLine = true,
                enabled = !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded && query.isNotBlank() && !isLoading,
                onDismissRequest = { expanded = false }
            ) {
                if (filteredFriends.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Crear amigo \"$query\"") },
                        enabled = !isCreating,
                        onClick = {
                            viewModel.createFriend(query) { created ->
                                expanded = false
                                onFriendSelected(created)
                            }
                        }
                    )
                } else {
                    filteredFriends.forEach { friend ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(friend.name)
                                    if (!friend.phone.isNullOrBlank()) {
                                        Text(
                                            text = friend.phone,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.onQueryChange(friend.name)
                                expanded = false
                                onFriendSelected(friend)
                            }
                        )
                    }
                }
            }
        }

        if (isCreating) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
    }
}
