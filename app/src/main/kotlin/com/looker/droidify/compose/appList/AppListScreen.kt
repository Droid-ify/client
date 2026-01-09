package com.looker.droidify.compose.appList

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.looker.droidify.data.model.AppMinimal
import com.looker.droidify.sync.v2.model.DefaultName

@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val apps by viewModel.appsState.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsState()

    val availableCategories by viewModel.categories.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            AppListTopBar(
                onNavigateToRepos = onNavigateToRepos,
                onNavigateToSettings = onNavigateToSettings,
                title = { SearchBar(viewModel.searchQuery) },
            )
        }
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
        ) {
            stickyHeader {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val favSelected by viewModel.favouritesOnly.collectAsStateWithLifecycle()
                        FilterChip(
                            selected = favSelected,
                            onClick = { viewModel.toggleFavouritesOnly() },
                            label = { Text("Favourites") },
                        )
                    }
                    CategoriesList(availableCategories) { category ->
                        CategoryChip(
                            category = category,
                            selected = category in selectedCategories,
                            onToggle = { viewModel.toggleCategory(category) },
                        )
                    }
                }
            }
            items(
                items = apps,
                key = { it.appId },
            ) { app ->
                AppItem(
                    app = app,
                    onClick = { onAppClick(app.packageName.name) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedTransition = updateTransition(isPressed)
    val pressedScale by pressedTransition.animateFloat(label = "pressedScale") { isPressed ->
        if (isPressed) 0.97F else 1F
    }
    val pressedShape by pressedTransition.animateDp(label = "pressedShape") { isPressed ->
        if (isPressed) 20.dp else 32.dp
    }
    BasicTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = LocalTextStyle.current,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(pressedShape)
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(modifier),
        decorator = {
            Box(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val isFocused by interactionSource.collectIsFocusedAsState()
                if (state.text.isEmpty()) {
                    val colors = TextFieldDefaults.colors()
                    val color by animateColorAsState(
                        if (!isFocused) {
                            colors.focusedPlaceholderColor
                        } else {
                            colors.unfocusedPlaceholderColor
                        },
                    )
                    Text(
                        text = "Search",
                        color = color,
                    )
                }
                it()
            }
        },
    )
}

@Composable
fun CategoriesList(
    categories: List<DefaultName>,
    modifier: Modifier = Modifier,
    content: @Composable (DefaultName) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(categories) { category ->
            content(category)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListTopBar(
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
    title: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = title,
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Repositories") },
                    onClick = {
                        expanded = false
                        onNavigateToRepos()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        expanded = false
                        onNavigateToSettings()
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(
    category: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(category) },
    )
}

@Composable
private fun AppItem(
    app: AppMinimal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .then(modifier),
    ) {
        Spacer(modifier = Modifier.size(12.dp))
        var icon by remember { mutableStateOf(app.icon?.path) }
        if (icon != null) {
            AsyncImage(
                model = icon,
                onError = { icon = app.fallbackIcon?.path },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1F, true)
                    .clip(MaterialTheme.shapes.small),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1F, true)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small
                    ),
            ) {
                Image(
                    painter = painterResource(android.R.mipmap.sym_def_app_icon),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1F)) {
            Row {
                val (versionColor, backgroundColor) = app.versionColors()
                Text(
                    text = app.name,
                    maxLines = 1,
                    modifier = Modifier.weight(1F),
                )
                Text(
                    text = app.suggestedVersion,
                    style = MaterialTheme.typography.labelMedium,
                    color = versionColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .wrapContentHeight()
                        .widthIn(max = 96.dp)
                        .background(color = backgroundColor, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            app.summary?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
fun AppMinimal.versionColors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return scheme.outline to scheme.background
}
