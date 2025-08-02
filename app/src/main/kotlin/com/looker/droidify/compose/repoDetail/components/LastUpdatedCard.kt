package com.looker.droidify.compose.repoDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LastUpdatedCard(
    timestamp: Long,
    modifier: Modifier = Modifier,
) {
    val lastUpdated = remember(timestamp) {
        buildAnnotatedString {
            append("Last synced: ")
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append(formatDate(timestamp))
            }
        }
    }


    val isToday by remember(timestamp) { derivedStateOf { isToday(timestamp) } }
    val backgroundColor = if (isToday) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (isToday) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = lastUpdated,
        style = MaterialTheme.typography.bodyLarge,
        color = contentColor,
        modifier = Modifier
            .background(backgroundColor, MaterialTheme.shapes.small)
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .then(modifier),
    )
}

fun isToday(timestamp: Long): Boolean = android.text.format.DateUtils.isToday(timestamp)

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val pattern = if (isToday(timestamp)) "HH:mm" else "yyyy-MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}
