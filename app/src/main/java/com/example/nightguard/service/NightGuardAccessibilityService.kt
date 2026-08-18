package com.example.nightguard.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.nightguard.core.NightModeManager
import com.example.nightguard.data.WhitelistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NightGuardAccessibilityService :
    AccessibilityService() {

    companion object {
        private const val TAG = "NightGuardAccessibility"
    }

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    private lateinit var whitelistRepository: WhitelistRepository
    private lateinit var nightModeManager: NightModeManager

    @Volatile
    private var whitelist: Set<String> = emptySet()

    override fun onServiceConnected() {

        super.onServiceConnected()

        whitelistRepository =
            WhitelistRepository(this)

        nightModeManager =
            NightModeManager(this)

        Log.d(
            TAG,
            "Accessibility service connected"
        )

        Log.d(
            TAG,
            "NightGuard package = $packageName"
        )

        serviceScope.launch {

            whitelistRepository
                .whitelistFlow()
                .collectLatest { packages ->

                    whitelist = packages

                    Log.d(
                        TAG,
                        "Whitelist updated: $packages"
                    )
                }
        }
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {

        if (event == null) {
            return
        }

        /*
         * We only care about changes to the
         * currently visible window/application.
         */
        if (
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val packageName =
            event.packageName
                ?.toString()
                ?: return

        Log.d(
            TAG,
            "Window changed: $packageName"
        )

        enforcePolicy(packageName)
    }

    private fun enforcePolicy(
        packageName: String
    ) {

        /*
         * ==================================================
         * 1. NEVER BLOCK NIGHTGUARD
         * ==================================================
         *
         * This must always be the first check.
         *
         * It allows:
         *
         * - NightGuard itself
         * - Search
         * - Text fields
         * - Settings
         * - Any internal NightGuard screen
         */
        if (packageName == this.packageName) {

            Log.d(
                TAG,
                "Allowing NightGuard"
            )

            return
        }

        /*
         * ==================================================
         * 2. DAY MODE
         * ==================================================
         *
         * Nothing is blocked during Day Mode.
         */
        if (!nightModeManager.isNightMode()) {

            return
        }

        /*
         * ==================================================
         * 3. WHITELIST
         * ==================================================
         *
         * Launcher, Phone, System UI, NightGuard, etc.
         */
        if (whitelist.contains(packageName)) {

            Log.d(
                TAG,
                "Allowing whitelisted app: $packageName"
            )

            return
        }

        /*
         * ==================================================
         * 4. SYSTEM / KEYBOARD / TRANSIENT WINDOWS
         * ==================================================
         *
         * AccessibilityService can receive events from:
         *
         * - keyboards
         * - input methods
         * - system components
         * - transient windows
         *
         * These are NOT apps that we want to block.
         */
        if (isSystemOrInputMethod(packageName)) {

            Log.d(
                TAG,
                "Ignoring system/IME package: $packageName"
            )

            return
        }

        /*
         * ==================================================
         * 5. ONLY BLOCK REAL LAUNCHABLE APPLICATIONS
         * ==================================================
         *
         * If the package cannot be launched as a normal
         * application, don't treat it as an app that should
         * be blocked.
         */
        if (!isLaunchableApplication(packageName)) {

            Log.d(
                TAG,
                "Ignoring non-launchable package: $packageName"
            )

            return
        }

        /*
         * ==================================================
         * 6. BLOCK
         * ==================================================
         */
        Log.d(
            TAG,
            "BLOCKING app: $packageName"
        )

        performGlobalAction(
            GLOBAL_ACTION_HOME
        )
    }

    /**
     * Determines whether the package belongs to:
     *
     * - Android system
     * - an enabled keyboard / input method
     *
     * These packages can generate accessibility/window
     * events while another application is being used.
     */
    private fun isSystemOrInputMethod(
        packageName: String
    ): Boolean {

        /*
         * ------------------------------------------
         * Check enabled keyboards / input methods.
         * ------------------------------------------
         */
        val inputMethodManager =
            getSystemService(
                INPUT_METHOD_SERVICE
            ) as? InputMethodManager

        if (
            inputMethodManager
                ?.enabledInputMethodList
                ?.any { info ->
                    info.packageName == packageName
                } == true
        ) {

            return true
        }

        /*
         * ------------------------------------------
         * Check Android system application.
         * ------------------------------------------
         */
        val applicationInfo =
            try {

                packageManager.getApplicationInfo(
                    packageName,
                    0
                )

            } catch (e: Exception) {

                /*
                 * If Android cannot resolve the package,
                 * don't block it.
                 */
                return true
            }

        return (
                applicationInfo.flags and
                        ApplicationInfo.FLAG_SYSTEM
                ) != 0
    }

    /**
     * Determines whether this package represents a normal
     * launchable application.
     *
     * This is important because AccessibilityService can
     * receive events from components which are not actually
     * applications that the user launched.
     */
    private fun isLaunchableApplication(
        packageName: String
    ): Boolean {

        return try {

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            launchIntent != null

        } catch (e: Exception) {

            false
        }
    }

    override fun onInterrupt() {

        Log.d(
            TAG,
            "Accessibility service interrupted"
        )
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "Accessibility service destroyed"
        )

        serviceScope.cancel()

        super.onDestroy()
    }
}