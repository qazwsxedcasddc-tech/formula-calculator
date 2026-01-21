package com.formulacalc.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.ui.tabs.CalculatorTab
import com.formulacalc.ui.tabs.EngineeringTab
import com.formulacalc.ui.tabs.FormulasTab
import com.formulacalc.ui.tabs.GreekTab
import com.formulacalc.viewmodel.TabIndex

/**
 * Зона C: Панель ввода.
 *
 * Динамически меняется в зависимости от выбранной вкладки.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InputPanel(
    selectedTab: TabIndex,
    onTokenClick: (FormulaToken) -> Unit,
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onEqualsClick: () -> Unit,
    onPresetClick: (PresetFormula) -> Unit,
    onPresetDoubleTap: (PresetFormula) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedTab,
        modifier = modifier,
        transitionSpec = {
            // Анимация переключения вкладок
            if (targetState.index > initialState.index) {
                // Слайд влево
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                // Слайд вправо
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }.using(SizeTransform(clip = false))
        },
        label = "InputPanelContent"
    ) { tab ->
        when (tab) {
            TabIndex.CALCULATOR -> {
                CalculatorTab(
                    onDigitClick = onDigitClick,
                    onDecimalClick = onDecimalClick,
                    onClearClick = onClearClick,
                    onBackspaceClick = onBackspaceClick,
                    onEqualsClick = onEqualsClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            TabIndex.ENGINEERING -> {
                EngineeringTab(
                    onTokenClick = onTokenClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            TabIndex.GREEK -> {
                GreekTab(
                    onTokenClick = onTokenClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            TabIndex.FORMULAS -> {
                FormulasTab(
                    onPresetClick = onPresetClick,
                    onPresetDoubleTap = onPresetDoubleTap,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
