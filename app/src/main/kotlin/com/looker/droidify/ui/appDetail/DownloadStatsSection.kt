package com.looker.droidify.ui.appDetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.looker.droidify.R
import com.looker.droidify.compose.components.MonthlyLineChart
import com.looker.droidify.compose.components.MultiLineChart
import com.looker.droidify.compose.components.SimpleLineChart
import com.looker.droidify.data.local.model.DownloadStats

@Composable
fun DownloadStatsSection(
    downloadStats: List<DownloadStats>,
    modifier: Modifier = Modifier,
) {
    val dailyMap = remember(downloadStats) { downloadStats.toDailyMap() }
    val monthlyMap = remember(downloadStats) { downloadStats.toMonthlyMap() }

    if (dailyMap.isEmpty()) return

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.download_stats),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.download_stats_from_izzy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text(stringResource(R.string.total)) },
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text(stringResource(R.string.by_client)) },
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text(stringResource(R.string.monthly)) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = selectedTab,
                label = "chart_content",
            ) { tab ->
                when (tab) {
                    0 -> SimpleLineChart(dailyMap)
                    1 -> MultiLineChart(dailyMap)
                    2 -> MonthlyLineChart(monthlyMap)
                }
            }
        }
    }
}

private fun List<DownloadStats>.toDailyMap(): Map<String, Map<String, Long>> {
    return groupBy { it.date }
        .mapKeys { (dateInt, _) -> intToIsoDate(dateInt) }
        .mapValues { (_, stats) ->
            stats.associate { it.client to it.count }
        }
        .toSortedMap()
}

private fun List<DownloadStats>.toMonthlyMap(): Map<String, Map<String, Long>> {
    return groupBy { it.date / 100 } // YYYYMM
        .mapKeys { (monthInt, _) -> intToIsoMonth(monthInt) }
        .mapValues { (_, stats) ->
            stats
                .groupBy { it.client }
                .mapValues { (_, clientStats) -> clientStats.sumOf { it.count } }
        }
        .toSortedMap()
}

private fun intToIsoDate(date: Int): String {
    val year = date / 10000
    val month = (date / 100) % 100
    val day = date % 100
    return "%04d-%02d-%02d".format(year, month, day)
}

private fun intToIsoMonth(month: Int): String {
    val year = month / 100
    val monthOfYear = month % 100
    return "%04d-%02d-00".format(year, monthOfYear)
}
