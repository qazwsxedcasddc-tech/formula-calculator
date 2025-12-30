package com.formulacalc.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.formulacalc.viewmodel.TabIndex

/**
 * Зона B: Вкладки режимов.
 *
 * 4 вкладки: Калькулятор | Инженерный | Греческие | Формулы
 */
@Composable
fun TabsRow(
    selectedTab: TabIndex,
    onTabSelected: (TabIndex) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.index,
        modifier = modifier,
        edgePadding = TabRowDefaults.ScrollableTabRowPadding,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        TabIndex.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
