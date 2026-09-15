package com.didiprogrammer.youtepresta.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import com.didiprogrammer.youtepresta.ui.theme.statusColors

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
    val dueLoansCount by viewModel.dueLoansCount.collectAsState()
    var isDueLoansBannerDismissed by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.home_sign_out))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val showDueLoansBanner = (dueLoansCount ?: 0) > 0 && !isDueLoansBannerDismissed
            if (showDueLoansBanner) {
                DueLoansBanner(
                    count = dueLoansCount ?: 0,
                    onClick = onViewLoans,
                    onDismiss = { isDueLoansBannerDismissed = true },
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(
                    listOf(
                        SectionCardData(
                            icon = Icons.Filled.AccountBalanceWallet,
                            titleRes = R.string.sources_title,
                            count = fundingSourcesCount,
                            countPluralsRes = R.plurals.home_sources_count,
                            onClick = onViewFundingSources
                        ),
                        SectionCardData(
                            icon = Icons.Filled.Groups,
                            titleRes = R.string.friends_title,
                            count = friendsCount,
                            countPluralsRes = R.plurals.home_friends_count,
                            onClick = onViewFriends
                        ),
                        SectionCardData(
                            icon = Icons.Filled.Payments,
                            titleRes = R.string.loans_title,
                            count = activeLoansCount,
                            countPluralsRes = R.plurals.home_loans_count,
                            onClick = onViewLoans
                        )
                    )
                ) { card ->
                    SectionCard(card)
                }
            }
        }
    }
}

@Composable
private fun DueLoansBanner(
    count: Int,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.statusColors.warningContainer,
            contentColor = MaterialTheme.statusColors.onWarningContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, top = Spacing.xs, bottom = Spacing.xs, end = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_due_loans_banner, count),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.home_due_loans_banner_dismiss))
            }
        }
    }
}

private data class SectionCardData(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    val count: Int?,
    @PluralsRes val countPluralsRes: Int,
    val onClick: () -> Unit
)

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
            Text(text = stringResource(data.titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (data.count != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = pluralStringResource(data.countPluralsRes, data.count, data.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
