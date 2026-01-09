package com.looker.droidify.compose.repoDetail.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
    timestamp: Long?    ,
    modifier: Modifier = Modifier,
) {
    val lastUpdated = remember(timestamp) {
        buildAnnotatedString {
            append("Last updated: ")
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                timestamp?.let { append(formatDate(it)) }
            }
        }
    }

    val isToday by remember(timestamp) { derivedStateOf { isToday(timestamp ?: 0) } }
    val transition = updateTransition(isToday)
    val backgroundColor by transition.animateColor {
        if (it) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    }
    val borderColor by transition.animateColor {
        if (it) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    }
    val contentColor by transition.animateColor {
        if (it) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor, MaterialTheme.shapes.large)
            .border(width = 1.dp, color = borderColor, shape = MaterialTheme.shapes.large)
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .then(modifier),
    ) {
        AnimatedVisibility(isToday) {
            Icon(
                imageVector = Icons.Default.Check,
                tint = contentColor,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = lastUpdated,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
        )
    }
}

fun isToday(timestamp: Long): Boolean = DateUtils.isToday(timestamp)

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val pattern = if (isToday(timestamp)) "HH:mm" else "yyyy-MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}
