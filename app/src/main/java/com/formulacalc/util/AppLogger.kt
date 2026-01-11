package com.formulacalc.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Логгер приложения — записывает все действия, ошибки и результаты
 * Логи можно скопировать и отправить для анализа
 */
object AppLogger {
    private const val TAG = "FormulaCalc"
    private const val MAX_LOG_SIZE = 500_000 // 500KB максимум
    private const val MAX_ENTRIES = 1000

    private val logEntries = mutableListOf<String>()
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Инициализация с контекстом для записи в файл
    fun init(context: Context) {
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists()) logsDir.mkdirs()

        val fileName = "formula_log_${fileDateFormat.format(Date())}.txt"
        logFile = File(logsDir, fileName)

        // Очищаем старый лог если слишком большой
        logFile?.let { file ->
            if (file.exists() && file.length() > MAX_LOG_SIZE) {
                file.writeText("")
            }
        }

        log("APP", "═══════════════════════════════════════")
        log("APP", "Приложение запущено")
        log("APP", "Версия: Debug")
        log("APP", "═══════════════════════════════════════")
    }

    // Основной метод логирования (internal для использования в том же пакете)
    internal fun log(category: String, message: String, isError: Boolean = false) {
        val timestamp = dateFormat.format(Date())
        val emoji = when {
            isError -> "❌"
            category == "ACTION" -> "👆"
            category == "DRAG" -> "🔄"
            category == "CALC" -> "🔢"
            category == "RESULT" -> "✅"
            category == "UI" -> "🎨"
            category == "ERROR" -> "❌"
            category == "APP" -> "📱"
            category == "VALUE" -> "💾"
            category == "DEBUG" -> "🔍"
            category == "UNDO" -> "↩️"
            else -> "📝"
        }

        val entry = "[$timestamp] $emoji $category: $message"

        // В Android Log
        if (isError) {
            Log.e(TAG, entry)
        } else {
            Log.d(TAG, entry)
        }

        // В память
        synchronized(logEntries) {
            logEntries.add(entry)
            if (logEntries.size > MAX_ENTRIES) {
                logEntries.removeAt(0)
            }
        }

        // В файл
        try {
            logFile?.appendText("$entry\n")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка записи лога: ${e.message}")
        }
    }

    // === Действия пользователя ===

    fun userTap(element: String, details: String = "") {
        log("ACTION", "Тап на $element${if (details.isNotEmpty()) " ($details)" else ""}")
    }

    fun userLongPress(element: String, details: String = "") {
        log("ACTION", "Долгое нажатие на $element${if (details.isNotEmpty()) " ($details)" else ""}")
    }

    fun userDragStart(element: String, from: String) {
        log("DRAG", "Начало перетаскивания: $element из $from")
    }

    fun userDragMove(element: String, over: String?) {
        if (over != null) {
            log("DRAG", "Перетаскивание $element над $over")
        }
    }

    fun userDragEnd(element: String, target: String?, side: String?) {
        when {
            target != null && side != null -> {
                log("DRAG", "Завершение перетаскивания: $element → $target ($side)")
            }
            side == "RETURN_TO_PLACE" -> {
                log("DRAG", "Элемент возвращён на место: $element")
            }
            side == "DELETED" -> {
                log("DRAG", "Элемент удалён (перетащен за пределы): $element")
            }
            else -> {
                log("DRAG", "Перетаскивание отменено: $element")
            }
        }
    }

    fun userDropPreset(presetName: String) {
        log("ACTION", "Добавлена формула: $presetName")
    }

    fun userSelectOperator(operator: String, targetId: String) {
        log("ACTION", "Выбран оператор: $operator для $targetId")
    }

    fun userInputValue(variableName: String, variableId: String, value: Double?) {
        if (value != null) {
            log("VALUE", "Введено значение: $variableName = $value (id: $variableId)")
        } else {
            log("VALUE", "Очищено значение: $variableName (id: $variableId)")
        }
    }

    fun userReset() {
        log("ACTION", "Сброс формулы")
    }

    // === UI события ===

    fun dialogOpened(dialogName: String, details: String = "") {
        log("UI", "Открыт диалог: $dialogName${if (details.isNotEmpty()) " ($details)" else ""}")
    }

    fun dialogClosed(dialogName: String) {
        log("UI", "Закрыт диалог: $dialogName")
    }

    fun screenOpened(screenName: String) {
        log("UI", "Открыт экран: $screenName")
    }

    fun tabSelected(tabName: String) {
        log("UI", "Выбрана вкладка: $tabName")
    }

    // === Вычисления ===

    fun calculationStarted(formula: String, variables: Map<String, Double>) {
        log("CALC", "Начало вычисления")
        log("CALC", "Формула: $formula")
        log("CALC", "Переменные: $variables")
    }

    fun calculationResult(result: Double, formulaString: String) {
        log("RESULT", "Результат: $result")
        log("RESULT", "Выражение: $formulaString")
    }

    fun calculationMissing(missingVars: Set<String>) {
        log("CALC", "Не заданы переменные: $missingVars")
    }

    fun calculationError(error: String, formula: String = "") {
        log("ERROR", "Ошибка вычисления: $error", isError = true)
        if (formula.isNotEmpty()) {
            log("ERROR", "Формула: $formula", isError = true)
        }
    }

    // === Формула ===

    fun formulaChanged(elements: String) {
        log("CALC", "Формула изменена: $elements")
    }

    fun formulaState(elements: String, variableValues: Map<String, Double>) {
        log("CALC", "Состояние формулы: $elements")
        log("CALC", "Значения переменных: $variableValues")
    }

    // === Undo/Redo ===

    fun undoAction(actionName: String) {
        log("UNDO", "Отменено: $actionName")
    }

    fun redoAction() {
        log("UNDO", "Повторено действие")
    }

    // === Отладка (Debug) ===

    fun debugBounds(elementId: String, left: Int, top: Int, right: Int, bottom: Int) {
        log("DEBUG", "Bounds[$elementId]: [$left,$top - $right,$bottom]")
    }

    fun debugFormulaAreaBounds(left: Int, top: Int, right: Int, bottom: Int) {
        log("DEBUG", "FormulaArea bounds: [$left,$top - $right,$bottom]")
    }

    fun debugDropPosition(fingerX: Int, fingerY: Int, isInside: Boolean) {
        log("DEBUG", "Drop позиция: ($fingerX, $fingerY), внутри области: $isInside")
    }

    fun debugElementsState(elementsCount: Int, variablesCount: Int, constantsCount: Int) {
        log("DEBUG", "Состояние: элементов=$elementsCount, переменных=$variablesCount, констант=$constantsCount")
    }

    // === Ошибки ===

    fun error(message: String, exception: Throwable? = null) {
        log("ERROR", message, isError = true)
        exception?.let {
            log("ERROR", "Stack: ${it.stackTraceToString().take(500)}", isError = true)
        }
    }

    // === Получение логов ===

    /**
     * Получить все логи в виде строки для копирования
     */
    fun getLogsAsString(): String {
        val header = """
            |═══════════════════════════════════════
            |FORMULA CALCULATOR - LOG EXPORT
            |Дата: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            |═══════════════════════════════════════
            |
        """.trimMargin()

        return synchronized(logEntries) {
            header + logEntries.joinToString("\n")
        }
    }

    /**
     * Получить последние N записей
     */
    fun getLastEntries(count: Int = 100): String {
        return synchronized(logEntries) {
            logEntries.takeLast(count).joinToString("\n")
        }
    }

    /**
     * Получить путь к файлу логов
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }

    /**
     * Очистить логи
     */
    fun clear() {
        synchronized(logEntries) {
            logEntries.clear()
        }
        logFile?.writeText("")
        log("APP", "Логи очищены")
    }

    /**
     * Получить логи из файла
     */
    fun getLogsFromFile(): String {
        return try {
            logFile?.readText() ?: "Файл логов не найден"
        } catch (e: Exception) {
            "Ошибка чтения логов: ${e.message}"
        }
    }
}
