package com.looker.droidify.compose.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.droidify.R
import com.looker.droidify.datastore.model.CustomButton
import com.looker.droidify.datastore.model.CustomButtonIcon
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomButtonEditor(
    existingButton: CustomButton?,
    onSave: (CustomButton) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by remember { mutableStateOf(existingButton?.label ?: "") }
    var urlTemplate by remember { mutableStateOf(existingButton?.urlTemplate ?: "") }
    var selectedIcon by remember { mutableStateOf(existingButton?.icon ?: CustomButtonIcon.LINK) }
    var showTemplates by remember { mutableStateOf(false) }

    val isValid = label.isNotBlank() && urlTemplate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = if (existingButton != null) {
                    stringResource(R.string.custom_button_edit)
                } else {
                    stringResource(R.string.custom_button_add)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.custom_button_label)) },
                    placeholder = { Text(stringResource(R.string.custom_button_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = urlTemplate,
                    onValueChange = { urlTemplate = it },
                    label = { Text(stringResource(R.string.custom_button_url)) },
                    placeholder = { Text(stringResource(R.string.custom_button_url_hint)) },
                    supportingText = {
                        Text(stringResource(R.string.custom_button_url_description))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.custom_button_templates),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showTemplates = !showTemplates }) {
                        Text(
                            if (showTemplates) stringResource(R.string.show_less)
                            else stringResource(R.string.show_more)
                        )
                    }
                }

                if (showTemplates) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CustomButton.TEMPLATES.forEach { template ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    label = template.label
                                    urlTemplate = template.urlTemplate
                                    selectedIcon = template.icon
                                },
                                label = { Text(template.label) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.custom_button_icon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CustomButtonIcon.entries.forEach { icon ->
                        IconOption(
                            icon = icon,
                            isSelected = selectedIcon == icon,
                            onClick = { selectedIcon = icon },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val button = CustomButton(
                        id = existingButton?.id ?: UUID.randomUUID().toString(),
                        label = label.trim(),
                        urlTemplate = urlTemplate.trim(),
                        icon = selectedIcon,
                    )
                    onSave(button)
                },
                enabled = isValid,
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

@Composable
private fun IconOption(
    icon: CustomButtonIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == CustomButtonIcon.TEXT_ONLY) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        } else {
            Icon(
                painter = painterResource(icon.toDrawableRes()),
                contentDescription = icon.name,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Stable
fun CustomButtonIcon.toDrawableRes(): Int = when (this) {
    CustomButtonIcon.LINK -> R.drawable.ic_public
    CustomButtonIcon.SEARCH -> R.drawable.ic_search
    CustomButtonIcon.PRIVACY -> R.drawable.ic_verified
    CustomButtonIcon.STORE -> R.drawable.ic_search
    CustomButtonIcon.CODE -> R.drawable.ic_code
    CustomButtonIcon.DOWNLOAD -> R.drawable.ic_download
    CustomButtonIcon.SHARE -> R.drawable.ic_share
    CustomButtonIcon.BUG -> R.drawable.ic_bug_report
    CustomButtonIcon.INFO -> R.drawable.ic_perm_device_information
    CustomButtonIcon.EMAIL -> R.drawable.ic_email
    CustomButtonIcon.PERSON -> R.drawable.ic_person
    CustomButtonIcon.HISTORY -> R.drawable.ic_history
    CustomButtonIcon.SETTINGS -> R.drawable.ic_tune
    CustomButtonIcon.TEXT_ONLY -> R.drawable.ic_public // Fallback, won't be used
}
