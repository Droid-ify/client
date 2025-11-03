package com.looker.droidify.compose.appDetail

import android.text.TextUtils.TruncateAt.END
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.looker.droidify.R
import com.looker.droidify.compose.components.BackButton
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Html
import com.looker.droidify.utility.text.toAnnotatedString

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

                    if (app!!.categories.isNotEmpty()) {
                        CategoriesRow(categories = app!!.categories)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = app!!.metadata.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (app!!.metadata.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val handler = LocalUriHandler.current
                        Text(
                            text = app!!.metadata.description.toAnnotatedString(
                                onUrlClick = { handler.openUri(it) }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
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
            tv.text = html
            LinkifyCompat.addLinks(tv, Linkify.ALL)
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
                    text = "Version: $version",
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(screenshots, key = { it.path }) { file ->
            val painter = rememberAsyncImagePainter(file.path)
            val imageState by painter.state.collectAsStateWithLifecycle()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(180.dp)
                    .widthIn(min = 90.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                when (imageState) {
                    is AsyncImagePainter.State.Error -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                        )
                    }

                    is AsyncImagePainter.State.Success -> {
                        Image(
                            painter = painter,
                            contentDescription = "screenshot",
                            modifier = Modifier.height(200.dp),
                            contentScale = ContentScale.FillHeight
                        )
                    }

                    else -> {}
                }
            }
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
                enabled = true,
                label = { Text(cat) },
            )
        }
    }
}
