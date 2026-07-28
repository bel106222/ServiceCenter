package ru.bel.servicecenter.rules

// Набор статических методов для проверки данных.
// Каждый метод возвращает null, если значение корректно, или строку с сообщением об ошибке.
object ValidationRules {

    // Проверка email
    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email обязателен"
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(email)) return "Некорректный email"
        if (email.length > 100) return "Email слишком длинный"
        return null
    }

    // Проверка пароля: минимум 6 символов, есть цифра и буква
    fun validatePassword(password: String): String? {
        if (password.length < 6) return "Минимум 6 символов"
        if (!password.any { it.isDigit() }) return "Пароль должен содержать цифру"
        if (!password.any { it.isLetter() }) return "Пароль должен содержать букву"
        return null
    }

    // Проверка телефона
    fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return "Телефон обязателен"
        val phoneRegex = Regex("^\\+7\\d{10}$")
        if (!phoneRegex.matches(phone)) return "Формат: +7XXXXXXXXXX"
        return null
    }

    // Проверка, что строка не пустая
    fun validateRequired(value: String, fieldName: String): String? {
        return if (value.isBlank()) "Поле '$fieldName' обязательно" else null
    }

    // Проверка максимальной длины строки
    fun validateLength(value: String, max: Int, fieldName: String): String? {
        return if (value.length > max) "Поле '$fieldName' не должно превышать $max символов" else null
    }

    // Проверка, что строка является числом (для полей, где ожидается число)
    fun validateNumber(value: String, fieldName: String): String? {
        return if (value.toDoubleOrNull() == null) "Поле '$fieldName' должно быть числом" else null
    }
}