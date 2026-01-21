package com.formulacalc.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.formulacalc.model.FormulaToken
import com.formulacalc.model.PresetFormula
import com.formulacalc.ui.tabs.ClassicCalculatorTab
import com.formulacalc.ui.tabs.CombinedFormulasTab
import com.formulacalc.ui.tabs.TwoPanelCalculatorTab
import com.formulacalc.ui.tabs.DrawerCalculatorTab
import com.formulacalc.viewmodel.CalculatorLayout
import com.formulacalc.viewmodel.TabIndex

/**
 * Зона C: Панель ввода.
 *
 * Динамически меняется в зависимости от выбранной вкладки и раскладки.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InputPanel(
    selectedTab: TabIndex,
    calculatorLayout: CalculatorLayout,
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
    // Для вариантов B и C вкладки не нужны - всё в одном экране
    when (calculatorLayout) {
        CalculatorLayout.TWO_PANEL -> {
            TwoPanelCalculatorTab(
                onDigitClick = onDigitClick,
                onDecimalClick = onDecimalClick,
                onClearClick = onClearClick,
                onBackspaceClick = onBackspaceClick,
                onEqualsClick = onEqualsClick,
                onTokenClick = onTokenClick,
                onPresetClick = onPresetClick,
                onPresetDoubleTap = onPresetDoubleTap,
                modifier = modifier
            )
        }
        CalculatorLayout.DRAWER -> {
            DrawerCalculatorTab(
                onDigitClick = onDigitClick,
                onDecimalClick = onDecimalClick,
                onClearClick = onClearClick,
                onBackspaceClick = onBackspaceClick,
                onEqualsClick = onEqualsClick,
                onTokenClick = onTokenClick,
                onPresetDoubleTap = onPresetDoubleTap,
                modifier = modifier
            )
        }
        CalculatorLayout.CLASSIC -> {
            // Вариант A: с вкладками
            AnimatedContent(
                targetState = selectedTab,
                modifier = modifier,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "InputPanelContent"
            ) { tab ->
                when (tab) {
                    TabIndex.CALCULATOR -> {
                        ClassicCalculatorTab(
                            onDigitClick = onDigitClick,
                            onDecimalClick = onDecimalClick,
                            onClearClick = onClearClick,
                            onBackspaceClick = onBackspaceClick,
                            onEqualsClick = onEqualsClick,
                            onTokenClick = onTokenClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TabIndex.FORMULAS -> {
                        CombinedFormulasTab(
                            onTokenClick = onTokenClick,
                            onPresetClick = onPresetClick,
                            onPresetDoubleTap = onPresetDoubleTap,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
