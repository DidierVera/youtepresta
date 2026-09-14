package com.didiprogrammer.youtepresta.ui.friends

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.didiprogrammer.youtepresta.data.model.Friend

/**
 * Temporary screen to exercise [FriendPicker] before the Phase 4 loan form exists.
 * Remove once the loan form embeds FriendPicker directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendPickerTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFriend by remember { mutableStateOf<Friend?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Probar FriendPicker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            FriendPicker(onFriendSelected = { selectedFriend = it })

            Spacer(modifier = Modifier.height(24.dp))

            val friend = selectedFriend
            if (friend != null) {
                Text(
                    text = "Amigo seleccionado: ${friend.name}",
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text("Ningún amigo seleccionado todavía.")
            }
        }
    }
}
