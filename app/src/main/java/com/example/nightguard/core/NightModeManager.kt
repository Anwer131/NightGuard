package com.example.nightguard.core

import android.content.Context
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NightModeManager(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "nightguard_preferences",
            Context.MODE_PRIVATE
        )

    companion object {

        private const val KEY_NIGHT_START = "night_start"
        private const val KEY_NIGHT_END = "night_end"

        private const val DEFAULT_NIGHT_START = "23:00"
        private const val DEFAULT_NIGHT_END = "07:00"

        private val TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm")
    }

    /**
     * Saves both night-mode start and end times.
     */
    fun saveNightSchedule(
        start: LocalTime,
        end: LocalTime
    ) {
        preferences.edit()
            .putString(
                KEY_NIGHT_START,
                start.format(TIME_FORMAT)
            )
            .putString(
                KEY_NIGHT_END,
                end.format(TIME_FORMAT)
            )
            .apply()
    }

    /**
     * Returns the configured night-mode start time.
     */
    fun getNightStart(): LocalTime {

        val value =
            preferences.getString(
                KEY_NIGHT_START,
                DEFAULT_NIGHT_START
            ) ?: DEFAULT_NIGHT_START

        return LocalTime.parse(
            value,
            TIME_FORMAT
        )
    }

    /**
     * Returns the configured night-mode end time.
     */
    fun getNightEnd(): LocalTime {

        val value =
            preferences.getString(
                KEY_NIGHT_END,
                DEFAULT_NIGHT_END
            ) ?: DEFAULT_NIGHT_END

        return LocalTime.parse(
            value,
            TIME_FORMAT
        )
    }

    /**
     * Updates only the night-mode start time.
     */
    fun setNightStart(time: LocalTime) {

        preferences.edit()
            .putString(
                KEY_NIGHT_START,
                time.format(TIME_FORMAT)
            )
            .apply()
    }

    /**
     * Updates only the night-mode end time.
     */
    fun setNightEnd(time: LocalTime) {

        preferences.edit()
            .putString(
                KEY_NIGHT_END,
                time.format(TIME_FORMAT)
            )
            .apply()
    }

    /**
     * Returns true when the current time falls
     * inside the configured night-mode period.
     *
     * Supports both:
     *
     * 09:00 -> 18:00
     *
     * and:
     *
     * 23:00 -> 07:00
     */
    fun isNightMode(): Boolean {

        val now = LocalTime.now()

        val start = getNightStart()
        val end = getNightEnd()

        return if (start < end) {

            // Same-day schedule.
            //
            // Example:
            // 09:00 -> 18:00
            //
            // Active from 09:00 inclusive
            // until 18:00 exclusive.

            now >= start && now < end

        } else {

            // Cross-midnight schedule.
            //
            // Example:
            // 23:00 -> 07:00
            //
            // Active from 23:00 -> midnight
            // OR midnight -> 07:00.

            now >= start || now < end
        }
    }
}