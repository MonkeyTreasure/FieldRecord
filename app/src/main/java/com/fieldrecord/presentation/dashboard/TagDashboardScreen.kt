package com.fieldrecord.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fieldrecord.domain.model.PhotoTagFacets
import com.fieldrecord.domain.model.PhotoTagFilter
import com.fieldrecord.domain.model.PhotoTagKind
import com.fieldrecord.domain.model.PhotoTagOption

@Composable
fun TagDashboardScreen(
    facets: PhotoTagFacets,
    onTagSelected: (PhotoTagFilter) -> Unit
) {
    val availablePages = remember(facets) {
        buildList {
            if (facets.categories.isNotEmpty()) add(PhotoTagKind.CATEGORY)
            if (facets.blocks.isNotEmpty()) add(PhotoTagKind.BLOCK)
            if (facets.locations.isNotEmpty()) add(PhotoTagKind.LOCATION)
        }
    }
    var selectedPage by remember { mutableStateOf(availablePages.firstOrNull() ?: PhotoTagKind.CATEGORY) }
    LaunchedEffect(availablePages) {
        if (selectedPage !in availablePages) selectedPage = availablePages.firstOrNull() ?: PhotoTagKind.CATEGORY
    }
    val options = when (selectedPage) {
        PhotoTagKind.CATEGORY -> facets.categories
        PhotoTagKind.BLOCK -> facets.blocks
        PhotoTagKind.LOCATION -> facets.locations
    }
    val selectedIndex = availablePages.indexOf(selectedPage).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard Tag", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.onBackground)
        if (availablePages.isNotEmpty()) {
            TabRow(
                selectedTabIndex = selectedIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = Color.White
                    )
                }
            ) {
                availablePages.forEach { page ->
                    Tab(
                        selected = selectedPage == page,
                        onClick = { selectedPage = page },
                        text = { Text(page.title(), maxLines = 1) }
                    )
                }
            }
            Text(
                text = "${selectedPage.title()} (${options.size})",
                style = MaterialTheme.typography.subtitle1,
                color = Color.White
            )
            options.forEach { option ->
                TagOptionCard(option) {
                    onTagSelected(PhotoTagFilter(selectedPage, option.key, option.label))
                }
            }
        }
    }
}

@Composable
private fun TagOptionCard(option: PhotoTagOption, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Text(
            text = option.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface
        )
    }
}

private fun PhotoTagKind.title(): String = when (this) {
    PhotoTagKind.CATEGORY -> "Kategori"
    PhotoTagKind.BLOCK -> "Blok"
    PhotoTagKind.LOCATION -> "Lokasi"
}
