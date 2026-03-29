package com.looker.droidify.compose.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.looker.droidify.R
import com.looker.droidify.datastore.model.BlacklistEntry

@Composable
fun BlacklistSettingItem(
    entries: List<BlacklistEntry>,
    onAddEntry: (BlacklistEntry) -> Unit,
    onUpdateEntry: (BlacklistEntry) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<BlacklistEntry?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_blacklist_section),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.app_blacklist_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(
                    onClick = {
                        editingEntry = null
                        showEditor = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.app_blacklist_add),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.app_blacklist_export)) },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            enabled = entries.isNotEmpty(),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.app_blacklist_import)) },
                            onClick = {
                                showMenu = false
                                onImport()
                            },
                        )
                    }
                }
            }
        }

        if (entries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            entries.forEach { entry ->
                BlacklistEntryItem(
                    entry = entry,
                    onEdit = {
                        editingEntry = entry
                        showEditor = true
                    },
                    onDelete = { onRemoveEntry(entry.id) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showEditor) {
        BlacklistEntryEditor(
            existingEntry = editingEntry,
            onSave = { entry ->
                if (editingEntry != null) {
                    onUpdateEntry(entry)
                } else {
                    onAddEntry(entry)
                }
                showEditor = false
                editingEntry = null
            },
            onDismiss = {
                showEditor = false
                editingEntry = null
            },
        )
    }
}

@Composable
private fun BlacklistEntryItem(
    entry: BlacklistEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onEdit)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_block),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.packagePattern.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.appNamePattern.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = { showDeleteConfirmation = true }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.confirmation)) },
            text = { Text(stringResource(R.string.app_blacklist_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun BlacklistEntryEditor(
    existingEntry: BlacklistEntry?,
    onSave: (BlacklistEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var packagePattern by remember { mutableStateOf(existingEntry?.packagePattern.orEmpty()) }
    var appNamePattern by remember { mutableStateOf(existingEntry?.appNamePattern.orEmpty()) }
    val canSave = packagePattern.isNotBlank() || appNamePattern.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingEntry == null) {
                    stringResource(R.string.app_blacklist_add)
                } else {
                    stringResource(R.string.custom_button_edit)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packagePattern,
                    onValueChange = { packagePattern = it },
                    label = { Text(stringResource(R.string.app_blacklist_package_pattern)) },
                    placeholder = { Text("com.example.*") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = appNamePattern,
                    onValueChange = { appNamePattern = it },
                    label = { Text(stringResource(R.string.app_blacklist_name_pattern)) },
                    placeholder = { Text("*example*") },
                    singleLine = true,
                )
                Text(
                    text = stringResource(R.string.app_blacklist_pattern_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        BlacklistEntry(
                            id = existingEntry?.id ?: java.util.UUID.randomUUID().toString(),
                            packagePattern = packagePattern,
                            appNamePattern = appNamePattern,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
