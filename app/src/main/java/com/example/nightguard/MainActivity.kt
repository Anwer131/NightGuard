package com.example.nightguard

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nightguard.apps.AppAdapter
import com.example.nightguard.apps.InstalledAppsManager
import com.example.nightguard.core.NightModeManager
import com.example.nightguard.data.WhitelistRepository
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var saveButton: Button
    private lateinit var saveScheduleButton: Button

    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button

    private lateinit var modeText: TextView
    private lateinit var scheduleText: TextView

    private lateinit var appAdapter: AppAdapter
    private lateinit var whitelistRepository: WhitelistRepository
    private lateinit var nightModeManager: NightModeManager

    private val timeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a")

    // ---------------------------------------------------------
    // Temporary schedule selections
    //
    // These values change when the user uses the time picker.
    // They are only persisted after SAVE SCHEDULE is pressed.
    // ---------------------------------------------------------

    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // -----------------------------------------------------
        // Views
        // -----------------------------------------------------

        recyclerView =
            findViewById(R.id.appRecyclerView)

        saveButton =
            findViewById(R.id.saveButton)

        saveScheduleButton =
            findViewById(R.id.saveScheduleButton)

        startTimeButton =
            findViewById(R.id.startTimeButton)

        endTimeButton =
            findViewById(R.id.endTimeButton)

        modeText =
            findViewById(R.id.modeText)

        scheduleText =
            findViewById(R.id.scheduleText)

        // -----------------------------------------------------
        // Managers / repositories
        // -----------------------------------------------------

        whitelistRepository =
            WhitelistRepository(this)

        nightModeManager =
            NightModeManager(this)

        // -----------------------------------------------------
        // Initial setup
        // -----------------------------------------------------

        loadCurrentSchedule()

        updateModeUI()

        setupTimeButtons()

        loadApps()
    }

    // =========================================================
    // SCHEDULE
    // =========================================================

    /**
     * Load the currently persisted schedule.
     */
    private fun loadCurrentSchedule() {

        selectedStartTime =
            nightModeManager.getNightStart()

        selectedEndTime =
            nightModeManager.getNightEnd()

        updateTimeButtons()
    }

    /**
     * Update the text shown on the start/end buttons.
     */
    private fun updateTimeButtons() {

        selectedStartTime?.let { time ->

            startTimeButton.text =
                time.format(timeFormatter)
        }

        selectedEndTime?.let { time ->

            endTimeButton.text =
                time.format(timeFormatter)
        }
    }

    /**
     * Configure schedule buttons.
     */
    private fun setupTimeButtons() {

        startTimeButton.setOnClickListener {

            if (nightModeManager.isNightMode()) {

                showLockedMessage()

                return@setOnClickListener
            }

            showStartTimePicker()
        }

        endTimeButton.setOnClickListener {

            if (nightModeManager.isNightMode()) {

                showLockedMessage()

                return@setOnClickListener
            }

            showEndTimePicker()
        }

        saveScheduleButton.setOnClickListener {

            saveSchedule()
        }
    }

    /**
     * Show the start-time picker.
     */
    private fun showStartTimePicker() {

        val current =
            selectedStartTime
                ?: LocalTime.of(23, 0)

        val dialog =
            TimePickerDialog(
                this,
                { _, hour, minute ->

                    selectedStartTime =
                        LocalTime.of(
                            hour,
                            minute
                        )

                    updateTimeButtons()
                },
                current.hour,
                current.minute,
                false
            )

        dialog.show()
    }

    /**
     * Show the end-time picker.
     */
    private fun showEndTimePicker() {

        val current =
            selectedEndTime
                ?: LocalTime.of(7, 0)

        val dialog =
            TimePickerDialog(
                this,
                { _, hour, minute ->

                    selectedEndTime =
                        LocalTime.of(
                            hour,
                            minute
                        )

                    updateTimeButtons()
                },
                current.hour,
                current.minute,
                false
            )

        dialog.show()
    }

    /**
     * Persist the selected schedule.
     */
    private fun saveSchedule() {

        // Never allow schedule changes during Night Mode.
        if (nightModeManager.isNightMode()) {

            showLockedMessage()

            return
        }

        val start =
            selectedStartTime

        val end =
            selectedEndTime

        // Both values must exist.
        if (start == null || end == null) {

            Toast.makeText(
                this,
                "Please select both times",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Prevent a schedule such as:
        //
        // 23:00 -> 23:00
        //
        // which would otherwise mean "all day".
        if (start == end) {

            Toast.makeText(
                this,
                "Start and end time cannot be the same",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Save through NightModeManager.
        nightModeManager.saveNightSchedule(
            start,
            end
        )

        // Refresh the UI using the persisted values.
        updateModeUI()

        Toast.makeText(
            this,
            "Night schedule saved",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Update the current mode and schedule displayed at the top.
     */
    private fun updateModeUI() {

        val isNight =
            nightModeManager.isNightMode()

        val start =
            nightModeManager.getNightStart()

        val end =
            nightModeManager.getNightEnd()

        val startText =
            start.format(timeFormatter)

        val endText =
            end.format(timeFormatter)

        if (isNight) {

            modeText.text =
                "NIGHT MODE ACTIVE"

        } else {

            modeText.text =
                "DAY MODE"
        }

        scheduleText.text =
            "Night mode: $startText - $endText"

        updateConfigurationState()
    }

    // =========================================================
    // APPLICATION LIST
    // =========================================================

    private fun loadApps() {

        val manager =
            InstalledAppsManager(this)

        val apps =
            manager.getInstalledApps()

        appAdapter =
            AppAdapter(apps)

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter =
            appAdapter

        loadSavedWhitelist()

        saveButton.setOnClickListener {

            saveWhitelist()
        }
    }

    private fun loadSavedWhitelist() {

        lifecycleScope.launch {

            val savedPackages =
                whitelistRepository
                    .getWhitelist()

            appAdapter.setSelectedPackages(
                savedPackages
            )

            updateConfigurationState()
        }
    }

    // =========================================================
    // LOCKING
    // =========================================================

    /**
     * Lock configuration while Night Mode is active.
     *
     * Locked:
     *
     * - whitelist
     * - start time
     * - end time
     * - save schedule
     */
    private fun updateConfigurationState() {

        val isNight =
            nightModeManager.isNightMode()

        if (isNight) {

            // -------------------------
            // NIGHT MODE
            // -------------------------

            saveButton.isEnabled = false
            recyclerView.isEnabled = false

            startTimeButton.isEnabled = false
            endTimeButton.isEnabled = false
            saveScheduleButton.isEnabled = false

            saveButton.text =
                "WHITELIST LOCKED AT NIGHT"

            saveScheduleButton.text =
                "SCHEDULE LOCKED AT NIGHT"

        } else {

            // -------------------------
            // DAY MODE
            // -------------------------

            saveButton.isEnabled = true
            recyclerView.isEnabled = true

            startTimeButton.isEnabled = true
            endTimeButton.isEnabled = true
            saveScheduleButton.isEnabled = true

            saveButton.text =
                "SAVE WHITELIST"

            saveScheduleButton.text =
                "SAVE SCHEDULE"
        }
    }

    // =========================================================
    // WHITELIST
    // =========================================================

    private fun saveWhitelist() {

        if (nightModeManager.isNightMode()) {

            Toast.makeText(
                this,
                "Whitelist cannot be changed during night mode",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val selectedPackages =
            appAdapter.getSelectedPackages()

        lifecycleScope.launch {

            whitelistRepository
                .saveWhitelist(selectedPackages)

            Toast.makeText(
                this@MainActivity,
                "Whitelist saved",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private fun showLockedMessage() {

        Toast.makeText(
            this,
            "Settings are locked during Night Mode",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onResume() {

        super.onResume()

        updateModeUI()

        if (::appAdapter.isInitialized) {

            updateConfigurationState()
        }
    }
}