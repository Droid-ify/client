package com.looker.droidify.compose.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.droidify.R
import com.looker.droidify.compose.settings.ImportExportOptions

@Composable
fun ImportExportDialog(
    options: ImportExportOptions,
    onOptionsChange: (ImportExportOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isImport: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (isImport) R.string.import_data_title else R.string.export_data_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                CheckboxItem(label = stringResource(R.string.checkbox_settings))

                CheckboxItem(label = stringResource(R.string.checkbox_favourites))

                CheckboxItem(label = stringResource(R.string.checkbox_history))

                CheckboxItem(
                    label = stringResource(R.string.checkbox_repos),
                    checked = options.repositories,
                    enabled = true,
                    onCheckedChange = {
                        onOptionsChange(options.copy(repositories = it))
                    },
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(
                            if (isImport) R.string.import_selected else R.string.export_selected,
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun CheckboxItem(
    label: String,
    checked: Boolean = true,
    enabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
