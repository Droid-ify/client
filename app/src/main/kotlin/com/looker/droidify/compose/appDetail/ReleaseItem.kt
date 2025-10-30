package com.looker.droidify.compose.appDetail

import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looker.droidify.R
import com.looker.droidify.data.local.model.Reproducible
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.network.DataSize
import com.looker.droidify.utility.common.sdkName
import com.looker.droidify.utility.extension.android.Android
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun ReleaseItem(
    release: Release,
    repository: Repository,
    installedItem: InstalledItem?,
    reproducible: Reproducible,
    showSignature: Boolean,
    suggested: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val incompatibility = remember(release) { release.incompatibilities.firstOrNull() }
    val singlePlatform = remember(release) {
        if (release.platforms.size == 1) release.platforms.first() else null
    }
    val installed = remember(release, installedItem) {
        installedItem?.versionCode == release.versionCode &&
            installedItem.signature == release.signature
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (suggested) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Version and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.version_FORMAT, release.version).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (installed || suggested) {
                        Surface(
                            color = if (installed) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (installed) R.string.installed else R.string.suggested
                                ).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (installed) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Date
                val dateString = remember(release.added) {
                    val instant = Instant.fromEpochMilliseconds(release.added)
                    val date = instant.toLocalDateTime(TimeZone.UTC)
                    try {
                        date.toJavaLocalDateTime()
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
                    } catch (_: Exception) {
                        DateFormat.getDateFormat(context).format(release.added)
                    }
                }
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            // Source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (reproducible != Reproducible.NO_DATA) Icon(
                    imageVector = when (reproducible) {
                        Reproducible.TRUE  -> Icons.Outlined.GppGood
                        Reproducible.FALSE -> Icons.Outlined.GppBad
                        else               -> Icons.Outlined.GppMaybe // Reproducible.UNKNOWN
                    },
                    contentDescription = stringResource(id = R.string.rb_badge),
                    tint = when (reproducible) {
                        Reproducible.TRUE  -> Color.Green
                        Reproducible.FALSE -> Color.Red
                        else               -> Color.Yellow//Warning
                    }
                )
                Text(
                    text = stringResource(R.string.provided_by_FORMAT, repository.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = DataSize(release.size).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            // Signature
            if (showSignature && release.signature.isNotEmpty()) {
                val signatureText = remember(release.signature) {
                    val bytes = release.signature
                        .uppercase(Locale.US)
                        .windowed(2, 2, false)
                        .take(8)
                    val signature = bytes.joinToString(separator = " ")
                    buildAnnotatedString {
                        append(context.getString(R.string.signature_FORMAT, ""))
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append(signature)
                        }
                    }
                }
                Text(
                    text = signatureText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // (In)Compatibility
            if (incompatibility != null || singlePlatform != null) {
                val compatibilityText = remember(incompatibility, singlePlatform) {
                    when {
                        incompatibility != null -> when (incompatibility) {
                            is Release.Incompatibility.MinSdk,
                            is Release.Incompatibility.MaxSdk ->
                                context.getString(R.string.incompatible_with_FORMAT, Android.name)

                            is Release.Incompatibility.Platform ->
                                context.getString(
                                    R.string.incompatible_with_FORMAT,
                                    Android.primaryPlatform ?: context.getString(R.string.unknown)
                                )

                            is Release.Incompatibility.Feature ->
                                context.getString(R.string.requires_FORMAT, incompatibility.feature)
                        }

                        singlePlatform != null ->
                            context.getString(R.string.only_compatible_with_FORMAT, singlePlatform)

                        else -> ""
                    }
                }
                Text(
                    text = compatibilityText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (incompatibility != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // SDK Version
            val sdkVersionText = remember(release.targetSdkVersion, release.minSdkVersion) {
                val targetSdkVersion = sdkName.getOrDefault(
                    release.targetSdkVersion,
                    context.getString(R.string.label_unknown_sdk, release.targetSdkVersion)
                )
                val minSdkVersion = sdkName.getOrDefault(
                    release.minSdkVersion,
                    context.getString(R.string.label_unknown_sdk, release.minSdkVersion)
                )
                context.getString(R.string.label_sdk_version, targetSdkVersion, minSdkVersion)
            }
            Text(
                text = sdkVersionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
