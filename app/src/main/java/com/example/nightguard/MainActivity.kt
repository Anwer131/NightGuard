package com.example.nightguard

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SearchView
import android.widget.Switch
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
import com.example.nightguard.service.NightGuardAccessibilityService
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var saveButton: Button
    private lateinit var saveScheduleButton: Button

    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button

    private lateinit var appSearchView: SearchView

    private lateinit var modeText: TextView
    private lateinit var scheduleText: TextView

    private lateinit var lockingSwitch: Switch
    private lateinit var appAdapter: AppAdapter
    private lateinit var whitelistRepository: WhitelistRepository
    private lateinit var nightModeManager: NightModeManager

    private val timeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a")

    // Temporary schedule values.
    // They are only persisted when SAVE SCHEDULE is pressed.
    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * NightGuard cannot function without Accessibility Service.
         *
         * Therefore, don't initialize the main application UI until
         * Accessibility permission has been granted.
         */
        if (isAccessibilityServiceEnabled()) {

            initializeMainScreen()

        } else {

            showAccessibilityRequiredScreen()
        }
    }

    /**
     * Check whether NightGuard's Accessibility Service is enabled.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {

        val componentName =
            ComponentName(
                this,
                NightGuardAccessibilityService::class.java
            )

        val enabledServices =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

        return enabledServices
            ?.split(":")
            ?.any {
                it.equals(
                    componentName.flattenToString(),
                    ignoreCase = true
                )
            } == true
    }

    /**
     * Show the screen displayed when Accessibility permission
     * has not yet been granted.
     */
    @SuppressLint("MissingInflatedId")
    private fun showAccessibilityRequiredScreen() {

        setContentView(
            R.layout.activity_accessibility_required
        )

        val enableButton =
            findViewById<Button>(
                R.id.enableAccessibilityButton
            )

        enableButton.setOnClickListener {

            startActivity(
                Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
            )
        }
    }

    /**
     * Initialize the actual NightGuard application UI.
     *
     * This is deliberately separated from onCreate() so that
     * the main UI is never shown while Accessibility is disabled.
     */
    private fun initializeMainScreen() {

        setContentView(R.layout.activity_main)

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

        lockingSwitch =
            findViewById(R.id.lockingSwitch)

        appSearchView =
            findViewById(R.id.appSearchView)

        whitelistRepository =
            WhitelistRepository(this)

        nightModeManager =
            NightModeManager(this)

        loadCurrentSchedule()

        updateModeUI()

        setupTimeButtons()

        loadApps()
        setupAppSearch()
        setupLockingSwitch()
    }

    /**
     * Load the currently saved schedule.
     */
    private fun loadCurrentSchedule() {

        selectedStartTime =
            nightModeManager.getNightStart()

        selectedEndTime =
            nightModeManager.getNightEnd()

        updateTimeButtons()
    }

    /**
     * Display selected schedule times.
     */
    private fun updateTimeButtons() {

        selectedStartTime?.let {

            startTimeButton.text =
                it.format(timeFormatter)
        }

        selectedEndTime?.let {

            endTimeButton.text =
                it.format(timeFormatter)
        }
    }

    /**
     * Configure schedule time picker buttons.
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
     * Save the configured Night Mode schedule.
     */
    private fun saveSchedule() {

        if (nightModeManager.isNightMode()) {

            showLockedMessage()

            return
        }

        val start =
            selectedStartTime

        val end =
            selectedEndTime

        if (start == null || end == null) {

            Toast.makeText(
                this,
                "Please select both times",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * Prevent a schedule such as:
         *
         * 11:00 PM → 11:00 PM
         *
         * because that would effectively mean Night Mode
         * is active for the entire day with the current
         * cross-midnight logic.
         */
        if (start == end) {

            Toast.makeText(
                this,
                "Start and end time cannot be the same",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        nightModeManager.saveNightSchedule(
            start,
            end
        )

        updateModeUI()

        Toast.makeText(
            this,
            "Night schedule saved",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Update the mode and schedule shown at the top.
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

    /**
     * Load installed applications.
     */
    private fun loadApps() {

        val manager =
            InstalledAppsManager(this)

        val apps =
            manager.getInstalledApps()

        val mandatoryPackages =
            getMandatoryPackages()

        appAdapter =
            AppAdapter(
                apps,
                mandatoryPackages
            )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter =
            appAdapter

        loadSavedWhitelist()

        saveButton.setOnClickListener {

            saveWhitelist()
        }
    }

    private fun setupAppSearch() {

        appSearchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {

                    appAdapter.filter(
                        query.orEmpty()
                    )

                    return true
                }

                override fun onQueryTextChange(
                    newText: String?
                ): Boolean {

                    appAdapter.filter(
                        newText.orEmpty()
                    )

                    return true
                }
            }
        )
    }

    /**
     * Load the previously saved whitelist.
     */
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

    /**
     * Lock schedule and whitelist during Night Mode.
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
            appSearchView.isEnabled = false

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
            appSearchView.isEnabled = true

            startTimeButton.isEnabled = true
            endTimeButton.isEnabled = true
            saveScheduleButton.isEnabled = true

            saveButton.text =
                "SAVE WHITELIST"

            saveScheduleButton.text =
                "SAVE SCHEDULE"
        }
    }

    /**
     * Save selected applications to the whitelist.
     */
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

    private fun getMandatoryPackages(): Set<String> {

        val mandatory =
            mutableSetOf<String>()

        val packageManager =
            packageManager

        // ----------------------------------------
        // NightGuard itself
        // ----------------------------------------

        mandatory.add(
            packageName
        )

        // ----------------------------------------
        // Default launcher
        // ----------------------------------------

        val launcherIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

        packageManager
            .resolveActivity(
                launcherIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            ?.activityInfo
            ?.packageName
            ?.let { launcherPackage ->

                mandatory.add(
                    launcherPackage
                )
            }

        // ----------------------------------------
        // Default phone / dialer
        // ----------------------------------------

        val dialIntent =
            Intent(Intent.ACTION_DIAL)

        packageManager
            .resolveActivity(
                dialIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            ?.activityInfo
            ?.packageName
            ?.let { dialerPackage ->

                mandatory.add(
                    dialerPackage
                )
            }

        // ----------------------------------------
        // Android System UI
        // ----------------------------------------

        mandatory.add(
            "com.android.systemui"
        )

        return mandatory
    }

    private fun setupLockingSwitch() {

        lockingSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (nightModeManager.isNightMode() && !isChecked) {

                lockingSwitch.isChecked = true

                Toast.makeText(
                    this,
                    "NightGuard cannot be disabled during Night Mode",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnCheckedChangeListener
            }

            nightModeManager.setLockingEnabled(isChecked)

            updateModeUI()
            updateLockingSwitch()
        }

        updateLockingSwitch()
    }

    private fun handleLockingSwitchChange(
        isChecked: Boolean
    ) {

        val currentlyNight =
            nightModeManager.isNightMode()

        if (currentlyNight && !isChecked) {

            // Don't allow disabling during Night Mode.
            lockingSwitch.setOnCheckedChangeListener(null)

            lockingSwitch.isChecked = true

            lockingSwitch.setOnCheckedChangeListener { _, checked ->
                handleLockingSwitchChange(checked)
            }

            Toast.makeText(
                this,
                "NightGuard cannot be disabled during Night Mode",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        nightModeManager.setLockingEnabled(
            isChecked
        )

        updateModeUI()
        updateLockingSwitch()
    }

    private fun updateLockingSwitch() {

        val isLocking =
            nightModeManager.isLockingEnabled()

        val isNight =
            nightModeManager.isNightMode()

        lockingSwitch.setOnCheckedChangeListener(null)

        if (isNight) {

            // Night Mode:
            // Always ON.
            lockingSwitch.isChecked = true

        } else {

            // Day Mode:
            // Use the user's saved state.
            lockingSwitch.isChecked = isLocking
        }

        lockingSwitch.setOnCheckedChangeListener { _, isChecked ->

            handleLockingSwitchChange(isChecked)
        }
    }

    /**
     * Show a message when configuration is locked.
     */
    private fun showLockedMessage() {

        Toast.makeText(
            this,
            "Settings are locked during Night Mode",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Called whenever the Activity comes back to the foreground.
     *
     * This is important because the user enables Accessibility
     * from Android Settings and then returns to NightGuard.
     */
    override fun onResume() {

        super.onResume()

        if (!isAccessibilityServiceEnabled()) {

            /*
             * Accessibility is still disabled.
             *
             * Keep the user on the required-permission screen.
             */
            showAccessibilityRequiredScreen()

            return
        }

        /*
         * Accessibility has now been enabled.
         *
         * If the main UI has not been initialized yet, initialize it.
         */
        if (!::appAdapter.isInitialized) {

            initializeMainScreen()

        } else {

            updateModeUI()

            updateConfigurationState()
        }
    }
}