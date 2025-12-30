package com.formulacalc.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formulacalc.model.PresetFormula
import com.formulacalc.model.presetFormulas
import com.formulacalc.ui.draggablePreset

/**
 * Вкладка "Формулы".
 *
 * Содержит готовые формулы:
 * - F = m · a
 * - v = s / t
 * - E = mc²
 * - и другие
 */
@Composable
fun FormulasTab(
    onPresetClick: (PresetFormula) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Заголовок
        Text(
            text = "Готовые формулы",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        // Список формул
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presetFormulas) { preset ->
                FormulaCard(
                    preset = preset,
                    onClick = { onPresetClick(preset) }
                )
            }
        }
    }
}

/**
 * Карточка готовой формулы с поддержкой drag & drop
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormulaCard(
    preset: PresetFormula,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .draggablePreset(preset, onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Название формулы
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Сама формула
            Text(
                text = preset.toDisplayString(),
                style = MaterialTheme.typography.headlineSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Описание
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
