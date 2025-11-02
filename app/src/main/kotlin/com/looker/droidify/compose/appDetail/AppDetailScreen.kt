package com.looker.droidify.compose.appDetail

import android.text.TextUtils.TruncateAt.END
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.looker.droidify.R
import com.looker.droidify.compose.components.BackButton
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Html
import com.looker.droidify.utility.text.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    onBackClick: () -> Unit,
    viewModel: AppDetailViewModel,
) {
    val app by viewModel.state.collectAsStateWithLifecycle()

    if (app != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (app?.metadata?.name != null) {
                            Text(text = app?.metadata?.name ?: packageName)
                        }
                    },
                    navigationIcon = { BackButton(onBackClick) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                HeaderSection(
                    app = app,
                    packageName = packageName,
                    isInstalled = app?.packages?.any { it.installed } == true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (app != null) {
                    val screenshots: List<FilePath> = remember(app!!.screenshots) {
                        buildList {
                            app!!.screenshots?.phone?.let { addAll(it) }
                            app!!.screenshots?.sevenInch?.let { addAll(it) }
                            app!!.screenshots?.tenInch?.let { addAll(it) }
                            app!!.screenshots?.tv?.let { addAll(it) }
                            app!!.screenshots?.wear?.let { addAll(it) }
                        }
                    }
                    if (screenshots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ScreenshotsRow(screenshots = screenshots)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = app!!.metadata.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (app!!.metadata.description.isNotBlank()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(8.dp))
                        HtmlText(
                            html = app!!.metadata.description,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 8,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        Button(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            if (isExpanded) {
                                Text(text = "Less")
                            } else {
                                Text(text = "More")
                            }
                        }
                    }

                    if (app!!.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoriesRow(categories = app!!.categories)
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "App details not available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HtmlText(
    html: Html,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toArgb()
    val linkColor = colors.primary.toArgb()
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true

                setTextColor(textColor)
                setLinkTextColor(linkColor)
            }
        },
        update = { tv ->
            tv.ellipsize = END
            tv.maxLines = maxLines
            tv.text = html.format()
        },
        modifier = modifier,
    )
}

@Composable
private fun HeaderSection(
    app: App?,
    packageName: String,
    isInstalled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val iconUrl = app?.metadata?.icon?.path
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_cannot_load),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app?.metadata?.name ?: packageName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val version = app?.metadata?.suggestedVersionName?.takeIf { it.isNotBlank() } ?: ""
            if (version.isNotEmpty()) {
                Text(
                    text = "Version $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = app?.author?.name ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Button(onClick = { /* Hook up downloads/installs later */ }) {
            Text(text = if (isInstalled) "Open" else "Get")
        }
    }
}

@Composable
private fun ScreenshotsRow(screenshots: List<FilePath>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(screenshots, key = { it.path }) { ss ->
            AsyncImage(
                model = ss.path,
                contentDescription = null,
                modifier = Modifier
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesRow(categories: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = false,
                onClick = { },
                enabled = false,
                label = { Text(cat) },
            )
        }
    }
}
