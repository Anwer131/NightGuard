package com.example.nightguard.scheduler

import android.content.Context
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NightGuardPreferences(context: Context) {

    private val preferences =
        context.getSharedPreferences("nightguard_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NIGHT_START = "night_start"
        private const val KEY_NIGHT_END = "night_end"

        private const val DEFAULT_NIGHT_START = "23:00"
        private const val DEFAULT_NIGHT_END = "07:00"

        private val FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    fun getNightStart(): LocalTime {
        val value = preferences.getString(
            KEY_NIGHT_START,
            DEFAULT_NIGHT_START
        ) ?: DEFAULT_NIGHT_START

        return LocalTime.parse(value, FORMATTER)
    }

    fun getNightEnd(): LocalTime {
        val value = preferences.getString(
            KEY_NIGHT_END,
            DEFAULT_NIGHT_END
        ) ?: DEFAULT_NIGHT_END

        return LocalTime.parse(value, FORMATTER)
    }

    fun setNightStart(time: LocalTime) {
        preferences.edit()
            .putString(KEY_NIGHT_START, time.format(FORMATTER))
            .apply()
    }

    fun setNightEnd(time: LocalTime) {
        preferences.edit()
            .putString(KEY_NIGHT_END, time.format(FORMATTER))
            .apply()
    }
}