package com.formulacalc.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Запись в истории вычислений
 */
data class CalculationEntry(
    val id: String = UUID.randomUUID().toString(),
    val formulaDescription: String, // Описание формулы (например "F = G·m₁·m₂/r²")
    val result: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val variables: Map<String, Double> = emptyMap() // Использованные значения переменных
) {
    fun getFormattedTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    fun getFormattedDate(): String {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    fun getFormattedResult(): String {
        return if (result == result.toLong().toDouble() && kotlin.math.abs(result) < 1e10) {
            result.toLong().toString()
        } else {
            String.format("%.6g", result)
        }
    }
}

/**
 * Менеджер истории вычислений
 */
class CalculationHistory(
    private val maxEntries: Int = 50
) {
    private val entries = mutableListOf<CalculationEntry>()

    /**
     * Добавить запись в историю
     */
    fun addEntry(
        formulaDescription: String,
        result: Double,
        variables: Map<String, Double>
    ) {
        val entry = CalculationEntry(
            formulaDescription = formulaDescription,
            result = result,
            variables = variables
        )
        entries.add(0, entry) // Добавляем в начало

        // Ограничиваем размер
        while (entries.size > maxEntries) {
            entries.removeLast()
        }

        AppLogger.log("HISTORY", "Добавлено в историю: ${entry.formulaDescription} = ${entry.getFormattedResult()}")
    }

    /**
     * Получить все записи
     */
    fun getAll(): List<CalculationEntry> = entries.toList()

    /**
     * Получить последние N записей
     */
    fun getLast(count: Int): List<CalculationEntry> = entries.take(count)

    /**
     * Очистить историю
     */
    fun clear() {
        entries.clear()
        AppLogger.log("HISTORY", "История очищена")
    }

    /**
     * Удалить запись по ID
     */
    fun remove(id: String) {
        entries.removeAll { it.id == id }
    }

    /**
     * Количество записей
     */
    fun size(): Int = entries.size

    /**
     * Пустая ли история
     */
    fun isEmpty(): Boolean = entries.isEmpty()
}
