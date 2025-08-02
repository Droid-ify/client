package com.looker.droidify.compose.repoList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.looker.droidify.data.model.Repo
import com.looker.droidify.utility.common.log

@Composable
fun RepoListScreen(
    viewModel: RepoListViewModel = viewModel(),
) {
    val repos by viewModel.stream.collectAsStateWithLifecycle()
    LazyColumn {
        items(repos) { repo ->
            RepoItem(
                onClick = { log(repo.icon?.path, "repo") },
                onToggle = { viewModel.toggleRepo(repo) },
                repo = repo,
            )
        }
    }
}

@Composable
private fun RepoItem(
    onClick: () -> Unit,
    onToggle: () -> Unit,
    repo: Repo,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
            .then(modifier),
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        AsyncImage(
            model = repo.icon?.path,
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1F, true)
                .clip(MaterialTheme.shapes.large),
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = repo.name,
                maxLines = 1,
            )
            Text(
                text = repo.description,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        FilledIconToggleButton(
            checked = repo.enabled,
            onCheckedChange = { onToggle() },
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
        }
        Spacer(modifier = Modifier.size(16.dp))
    }
}
