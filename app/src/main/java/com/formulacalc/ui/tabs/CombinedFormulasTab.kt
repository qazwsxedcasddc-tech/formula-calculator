package com.formulacalc.ui.tabs

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.model.greekSymbols
import com.formulacalc.model.presetFormulas
import com.formulacalc.ui.draggablePreset
import com.formulacalc.ui.draggableToken

/**
 * Объединённая вкладка "Формулы".
 *
 * Содержит:
 * - Верхняя часть: Греческие символы (α β γ θ λ μ ω Δ Σ π φ ψ ε τ ρ)
 * - Нижняя часть: Готовые формулы (F = m·a, v = s/t, E = mc² и т.д.)
 */
@Composable
fun CombinedFormulasTab(
    onTokenClick: (FormulaToken) -> Unit,
    onPresetClick: (PresetFormula) -> Unit,
    onPresetDoubleTap: (PresetFormula) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Верхняя часть: Греческие символы (фиксированная высота)
        Column(
            modifier = Modifier.height(140.dp)
        ) {
            Text(
                text = "Греческие символы",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(greekSymbols) { token ->
                    GreekSymbolButton(
                        text = token.displayText,
                        token = token,
                        onClick = { onTokenClick(token) }
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Нижняя часть: Готовые формулы (заполняет остаток)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Готовые формулы",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(presetFormulas) { preset ->
                    CompactFormulaCard(
                        preset = preset,
                        onClick = { onPresetClick(preset) },
                        onDoubleTap = { onPresetDoubleTap(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GreekSymbolButton(
    text: String,
    token: FormulaToken,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .draggableToken(token, onClick),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun CompactFormulaCard(
    preset: PresetFormula,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(preset) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .draggablePreset(preset, onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = preset.toDisplayString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
