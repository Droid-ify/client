package com.looker.droidify.compose.appDetail.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.looker.droidify.R
import com.looker.droidify.data.model.Package
import com.looker.droidify.utility.common.sdkName
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PackageItem(
    item: Package,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    label: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
            )
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Column(Modifier.weight(1F)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.version_FORMAT, item.manifest.versionName).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    label()
                }
                Text(
                    text = stringResource(
                        R.string.label_sdk_version,
                        sdkName[item.manifest.usesSDKs.target]!!,
                        sdkName[item.manifest.usesSDKs.min]!!
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val context = LocalContext.current
                val date = remember { formatDate(context, item.added) }
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = item.apk.size.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private fun formatDate(context: Context, instant: Long): String {
    val dateTime = LocalDateTime.ofEpochSecond(instant / 1000, 0, ZoneOffset.UTC)
    return try {
        dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    } catch (_: Exception) {
        DateFormat.getDateFormat(context).format(instant)
    }
}