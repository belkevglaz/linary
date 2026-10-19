package ru.b3lka.linary.presentation.platform

import ru.b3lka.linary.core.model.NewMedia

/**
 * Картинка, которую пользователь выбрал или вставил, но которая ещё не подготовлена к сохранению.
 */
fun interface ImageSource {

    /**
     * Уменьшает и сжимает картинку. Долго — вызывать не из UI-потока.
     * Возвращает null, если файл не удалось прочитать как картинку.
     */
    fun prepare(): NewMedia?
}
