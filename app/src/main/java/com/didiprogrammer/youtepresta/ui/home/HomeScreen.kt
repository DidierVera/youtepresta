package com.didiprogrammer.youtepresta.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    onViewFundingSources: () -> Unit,
    onViewFriends: () -> Unit,
    onViewLoans: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadCounts() }

    val fundingSourcesCount by viewModel.fundingSourcesCount.collectAsState()
    val friendsCount by viewModel.friendsCount.collectAsState()
    val activeLoansCount by viewModel.activeLoansCount.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Loans Tracker") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(
                listOf(
                    SectionCardData(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Bolsillos",
                        subtitle = fundingSourcesCount?.let { pluralize(it, "bolsillo", "bolsillos") },
                        onClick = onViewFundingSources
                    ),
                    SectionCardData(
                        icon = Icons.Filled.Groups,
                        title = "Amigos",
                        subtitle = friendsCount?.let { pluralize(it, "amigo", "amigos") },
                        onClick = onViewFriends
                    ),
                    SectionCardData(
                        icon = Icons.Filled.Payments,
                        title = "Préstamos",
                        subtitle = activeLoansCount?.let { pluralize(it, "activo", "activos") },
                        onClick = onViewLoans
                    )
                )
            ) { card ->
                SectionCard(card)
            }
        }
    }
}

private data class SectionCardData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit
)

private fun pluralize(count: Int, singular: String, plural: String): String =
    "$count ${if (count == 1) singular else plural}"

@Composable
private fun SectionCard(data: SectionCardData) {
    Card(
        onClick = data.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(text = data.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (data.subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
