package com.didiprogrammer.youtepresta.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.didiprogrammer.youtepresta.R
import com.didiprogrammer.youtepresta.ui.loans.DetailRow
import com.didiprogrammer.youtepresta.ui.loans.LoanRow
import com.didiprogrammer.youtepresta.ui.sources.formatCop
import com.didiprogrammer.youtepresta.ui.theme.Spacing
import com.didiprogrammer.youtepresta.util.PunctualityStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    friendId: String,
    onBack: () -> Unit,
    onLoanClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendDetailViewModel = viewModel()
) {
    LaunchedEffect(friendId) { viewModel.load(friendId) }

    val uiState by viewModel.uiState.collectAsState()
    val isDialogVisible by viewModel.isDialogVisible.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val isDeleteConfirmVisible by viewModel.isDeleteConfirmVisible.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val friendDeleted by viewModel.friendDeleted.collectAsState()

    LaunchedEffect(friendDeleted) {
        if (friendDeleted) onBack()
    }

    val content = uiState as? FriendDetailUiState.Content

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.friend_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = { viewModel.showEditDialog() }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.friend_edit_icon))
                        }
                        IconButton(onClick = { viewModel.confirmDelete() }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.friend_delete_icon))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FriendDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is FriendDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(state.messageRes), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Button(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is FriendDetailUiState.Content -> {
                FriendDetailContent(
                    state = state,
                    onLoanClick = onLoanClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (isDialogVisible && content != null) {
        FriendEditDialog(
            editingFriend = content.friend,
            isSaving = isSaving,
            saveError = saveError,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { name, phone, notes -> viewModel.saveFriend(name, phone, notes) }
        )
    }

    if (isDeleteConfirmVisible && content != null) {
        DeleteFriendConfirmDialog(
            friend = content.friend,
            isDeleting = isDeleting,
            deleteError = deleteError,
            onDismiss = { viewModel.dismissDeleteConfirmation() },
            onConfirmDelete = { viewModel.deleteFriend() }
        )
    }
}

@Composable
private fun FriendDetailContent(
    state: FriendDetailUiState.Content,
    onLoanClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg)
    ) {
        Text(text = state.friend.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (!state.friend.phone.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(text = state.friend.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(Spacing.md))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = stringResource(R.string.friend_summary_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                DetailRow(label = stringResource(R.string.friend_summary_current_debt_label), value = formatCop(state.summary.currentDebt))
                DetailRow(label = stringResource(R.string.friend_summary_active_loans_label), value = state.summary.activeLoanCount.toString())
                DetailRow(label = stringResource(R.string.friend_summary_total_loaned_label), value = formatCop(state.summary.totalLoanedHistoric))
                DetailRow(label = stringResource(R.string.friend_summary_total_interest_label), value = formatCop(state.summary.totalInterestCollected))
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.friend_punctuality_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PunctualityBadge(status = state.punctuality)
                }
                if (state.punctuality == PunctualityStatus.NOT_ENOUGH_DATA) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.friend_punctuality_not_enough_data_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.friend_loans_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (state.loanItems.isEmpty()) {
            Text(text = stringResource(R.string.friend_no_loans_state))
        } else {
            state.loanItems.forEach { item ->
                LoanRow(item = item, onClick = { onLoanClick(item.loan.id) })
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
    }
}
