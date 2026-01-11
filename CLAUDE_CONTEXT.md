# Formula Calculator - Контекст для Claude

## Проект
- **Путь**: `C:\Users\serge\formula-calculator`
- **GitHub**: https://github.com/qazwsxedcasddc-tech/formula-calculator
- **Пакет**: `com.formulacalc`
- **Память mem0**: Balalaika

## Технологии
- Kotlin
- Jetpack Compose
- Material 3
- Android SDK

## Структура проекта

```
app/src/main/java/com/formulacalc/
├── model/
│   ├── FormulaElement.kt      # Sealed class с типами элементов
│   └── FormulaUtils.kt        # Утилиты для работы с формулами
├── ui/formula/
│   ├── FormulaEditorScreen.kt # Главный экран
│   ├── FormulaRenderer.kt     # Compose UI для рендеринга формул
│   └── VariableInputDialog.kt # Диалог ввода значения переменной
├── viewmodel/
│   └── FormulaEditorViewModel.kt # ViewModel с бизнес-логикой
├── util/
│   ├── AppLogger.kt           # Логирование
│   ├── UndoRedoManager.kt     # Менеджер истории отмен
│   └── CalculationHistory.kt  # История вычислений
└── theme/
    └── Theme.kt               # Светлая и тёмная темы
```

## Типы элементов формулы (FormulaElement)

```kotlin
sealed class FormulaElement {
    data class Variable(id, value, displayValue, exponent)  // Переменная (a, b, x, m₁)
    data class Operator(id, type: OperatorType)             // Оператор (+, -, ×, ÷)
    data class Equals(id)                                   // Знак равенства
    data class Ellipsis(id)                                 // Placeholder для drop (···)
    data class Fraction(id, numerator, denominator)         // Дробь
    data class Parentheses(id, children)                    // Скобки-контейнер
}
```

## Реализованные функции

1. **Drag & Drop** - перетаскивание элементов формулы
2. **Переменные** - a, b, c, x, y, z, m₁, m₂, F, G, r
3. **Операторы** - +, -, ×, ÷, =
4. **Дроби** - числитель/знаменатель
5. **Степени** - простые и дробные (Exponent)
6. **Скобки** - контейнер для группировки (Parentheses)
7. **Вычисление** - ввод значений и расчёт результата
8. **Константы** - π, e, G, c, h с предустановленными значениями
9. **Undo/Redo** - отмена и повтор действий
10. **История** - история вычислений
11. **Копирование** - результат в буфер обмена
12. **Зона удаления** - визуализация при перетаскивании
13. **Snackbar** - уведомления с возможностью отмены
14. **Поделиться** - отправка формулы
15. **Тёмная тема** - автоматически по системе

## Цветовая схема

| Элемент | Цвет | HEX |
|---------|------|-----|
| Переменные | Синий | #3B82F6 |
| Константы | Жёлтый | #F59E0B |
| Операторы | Фиолетовый | #8B5CF6 |
| Дроби | Бирюзовый | #06B6D4 |
| Скобки | Изумрудный | #10B981 |

## Последняя сессия (12.01.2026)

### Что сделали:
1. Добавили `FormulaElement.Parentheses` - скобки как контейнер
2. Обновили все функции в FormulaUtils для поддержки скобок:
   - `clone()` - рекурсивное клонирование
   - `findById()` - поиск внутри скобок
   - `removeById()` - удаление с учётом скобок
   - `normalize()` - добавление ellipsis вокруг скобок
   - `insertHorizontal/Vertical()` - вставка внутрь скобок
   - `updateExponent()` - обновление степеней
   - `replaceEllipsis()` - замена placeholder
   - `wrapInParentheses()` - обернуть элемент в скобки
   - `wrapRangeInParentheses()` - обернуть диапазон
   - `addToParentheses()` - добавить элемент внутрь скобок
   - `toLogString()` - логирование скобок
3. Добавили `ParenthesesView` в FormulaRenderer.kt
4. Добавили кнопку "Обернуть в скобки" в VariableInputDialog
5. Добавили метод `wrapInParentheses()` в ViewModel
6. Исправили ошибку импорта `clone` в UndoRedoManager

### Как работают скобки:
- **Создание**: нажать на переменную → "( ) Обернуть в скобки"
- **Перетаскивание**: long press на скобках → drag всей группы
- **Удаление**: перетащить за пределы формулы

## Пользователь
- Теоретический физик
- Создаёт формулы "на лету" в процессе работы
- Важна возможность группировать существующие элементы постфактум

## Команды для сборки

```bash
# Проверить статус
cd "C:\Users\serge\formula-calculator" && git status

# Коммит и пуш
git add -A && git commit -m "message" && git push origin master

# Проверить билд
gh run list --repo qazwsxedcasddc-tech/formula-calculator --limit 1

# Скачать APK
gh run download <run_id> --repo qazwsxedcasddc-tech/formula-calculator -D "C:\Users\serge\Downloads"

# Установить на телефон
adb install -r "C:\Users\serge\Downloads\formula-calculator-debug\app-debug.apk"

# Если ошибка сигнатуры
adb uninstall com.formulacalc && adb install "path/to/apk"
```

## Что можно улучшить дальше

1. Выделение нескольких элементов для группировки
2. Drag элемента внутрь существующих скобок
3. Разворачивание скобок (убрать скобки, оставив содержимое)
4. Визуальная индикация при наведении на скобки
5. Анимации при группировке/разгруппировке
