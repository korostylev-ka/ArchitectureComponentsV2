package ru.netology.nmedia.dto

import androidx.annotation.StringRes
import ru.netology.nmedia.R

//класс для заполнения разделителей постов в зависимости от даты их создания
enum class TimesAgo(val time: Long, @StringRes val title: Int) {
    TODAY(0L, R.string.today),
    YESTERDAY(1L, R.string.yesterday),
    LAST_WEEK(2L, R.string.last_week),
    LONG_AGO(3L, R.string.long_ago);
}