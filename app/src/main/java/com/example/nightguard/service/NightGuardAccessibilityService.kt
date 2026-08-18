package com.example.nightguard.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.example.nightguard.BuildConfig
import com.example.nightguard.core.NightModeManager
import com.example.nightguard.data.WhitelistRepository

class NightGuardAccessibilityService :
    AccessibilityService() {

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

        serviceScope.launch {

            whitelistRepository
                .whitelistFlow()
                .collectLatest { packages ->

                    whitelist = packages
                }
        }
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {

        if (event == null) {
            return
        }

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

        enforcePolicy(packageName)
    }

    private fun enforcePolicy(
        packageName: String
    ) {

        // NightGuard itself must always remain accessible.
        if (packageName == BuildConfig.APPLICATION_ID) {
            return
        }

        // During daytime, don't block anything.
        if (!nightModeManager.isNightMode()) {
            return
        }

        // Whitelisted applications are allowed.
        if (whitelist.contains(packageName)) {
            return
        }

        // Everything else is blocked.
        performGlobalAction(
            GLOBAL_ACTION_HOME
        )
    }

    override fun onInterrupt() {
        // Nothing required here.
    }

    override fun onDestroy() {

        serviceScope.cancel()

        super.onDestroy()
    }
}