package com.looker.droidify.compose.appDetail

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.looker.droidify.R
import com.looker.droidify.compose.appDetail.components.CustomButtonsRow
import com.looker.droidify.compose.appDetail.components.PackageItem
import com.looker.droidify.compose.components.BackButton
import com.looker.droidify.data.model.App
import com.looker.droidify.data.model.FilePath
import com.looker.droidify.data.model.Package
import com.looker.droidify.data.model.Repo
import com.looker.droidify.datastore.model.CustomButton
import com.looker.droidify.utility.text.toAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    onBackClick: () -> Unit,
    viewModel: AppDetailViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customButtons by viewModel.customButtons.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (state) {
                        AppDetailState.Loading -> Text(stringResource(R.string.application))
                        is AppDetailState.Error -> {}
                        is AppDetailState.Success -> {
                            val app = (state as AppDetailState.Success).app
                            Text(text = app.metadata.name)
                        }
                    }
                },
                navigationIcon = { BackButton(onBackClick) },
            )
        },
    ) { padding ->
        when (state) {
            AppDetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is AppDetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = (state as AppDetailState.Error).message)
                }
            }

            is AppDetailState.Success -> {
                AppDetail(
                    app = (state as AppDetailState.Success).app,
                    packages = (state as AppDetailState.Success).packages,
                    customButtons = customButtons,
                    onCustomButtonClick = { url ->
                        try {
                            uriHandler.openUri(url)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AppDetail(
    app: App,
    packages: List<Pair<Package, Repo>>,
    customButtons: List<CustomButton>,
    onCustomButtonClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(modifier)
    ) {
        HeaderSection(
            app = app,
            packageName = app.metadata.packageName.name,
            isInstalled = app.packages?.any { it.installed } == true,
            isFavorite = true,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (customButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            CustomButtonsRow(
                buttons = customButtons,
                packageName = app.metadata.packageName.name,
                appName = app.metadata.name,
                authorName = app.author?.name,
                onButtonClick = onCustomButtonClick,
            )
        }

        val screenshots: List<FilePath> = remember(app.screenshots) {
            buildList {
                app.screenshots?.phone?.let { addAll(it) }
                app.screenshots?.sevenInch?.let { addAll(it) }
                app.screenshots?.tenInch?.let { addAll(it) }
                app.screenshots?.tv?.let { addAll(it) }
                app.screenshots?.wear?.let { addAll(it) }
            }
        }
        if (screenshots.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ScreenshotsRow(screenshots = screenshots)
        }

        if (app.categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            CategoriesRow(categories = app.categories)
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (app.metadata.summary.isNotBlank()) {
            Text(
                text = app.metadata.summary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (app.metadata.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val handler = LocalUriHandler.current
            Text(
                text = app.metadata.description.toAnnotatedString(
                    onUrlClick = { handler.openUri(it) }
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        val suggestedVersion = app.metadata.suggestedVersionCode
        packages.forEach { (pkg, repo) ->
            val isSuggested = pkg.manifest.versionCode == suggestedVersion
            PackageItem(
                item = pkg,
                repo = repo,
                onClick = {},
                onLongClick = {},
                backgroundColor = if (isSuggested) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surface
            ) {
                if (isSuggested) {
                    Text(
                        text = stringResource(R.string.suggested).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else if (pkg.installed) {
                    Text(
                        text = stringResource(R.string.suggested).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeaderSection(
    app: App?,
    packageName: String,
    isInstalled: Boolean,
    isFavorite: Boolean,
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

        FilledTonalIconToggleButton(
            checked = isFavorite,
            onCheckedChange = {},
            modifier = Modifier.size(
                IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Narrow)
            )
        ) {
            val icon = if (isFavorite) {
                R.drawable.ic_favourite_checked
            } else {
                R.drawable.ic_favourite
            }
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
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
