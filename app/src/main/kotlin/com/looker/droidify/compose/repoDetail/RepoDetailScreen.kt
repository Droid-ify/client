package com.looker.droidify.compose.repoDetail

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.looker.droidify.R
import com.looker.droidify.compose.repoDetail.components.LastUpdatedCard
import com.looker.droidify.compose.repoDetail.components.UnsyncedRepoState
import com.looker.droidify.data.model.Repo
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    viewModel: RepoDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val repo by viewModel.repo.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.bindService(context)
        onDispose {
            viewModel.unbindService(context)
        }
    }

    if (showDeleteDialog) {
        DeleteRepositoryDialog(
            onConfirm = {
                viewModel.deleteRepository {
                    onBackClick()
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.repository)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(viewModel.repoId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            repo == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.repository_not_found))
                }
            }

            repo!!.versionInfo == null -> {
                UnsyncedRepoState(
                    onEnableClick = { viewModel.enableRepository(true) },
                    address = repo!!.address,
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                RepoDetails(
                    onToggle = { viewModel.enableRepository(it) },
                    repo = repo!!,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun RepoDetails(
    onToggle: (Boolean) -> Unit,
    repo: Repo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .then(modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (repo.enabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = repo.enabled,
                onCheckedChange = onToggle,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val address = remember {
            buildAnnotatedString {
                withLink(LinkAnnotation.Url(repo.address)) {
                    append(repo.address)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = repo.name,
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primaryContainer,
            textDecoration = TextDecoration.Underline,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = repo.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (repo.versionInfo != null) {
            LastUpdatedCard(repo.versionInfo.timestamp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        FingerprintCard(
            title = stringResource(R.string.fingerprint),
            content = formatFingerprint(repo),
        )
    }
}

@Composable
private fun FingerprintCard(
    title: String,
    content: AnnotatedString,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DeleteRepositoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Repository") },
        text = { Text("Are you sure you want to delete this repository?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun formatFingerprint(repo: Repo): AnnotatedString {
    return repo.fingerprint?.let { fingerprint ->
        buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append(
                    fingerprint.value.windowed(2, 2, false)
                        .take(32).joinToString(separator = " ") { it.uppercase(Locale.US) },
                )
            }
        }
    } ?: buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
            append(stringResource(R.string.repository_unsigned_DESC))
        }
    }
}
