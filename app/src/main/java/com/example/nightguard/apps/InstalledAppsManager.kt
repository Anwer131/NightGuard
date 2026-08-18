package com.example.nightguard.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.nightguard.model.AppInfo

class InstalledAppsManager(
    private val context: Context
) {

    private val packageManager: PackageManager =
        context.packageManager

    fun getInstalledApps(): List<AppInfo> {

        val applications =
            packageManager.getInstalledApplications(
                PackageManager.GET_META_DATA
            )

        return applications
            .filter { isUserApp(it) }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    appName = packageManager
                        .getApplicationLabel(it)
                        .toString(),
                    isSystemApp = false
                )
            }
            .sortedBy {
                it.appName.lowercase()
            }
    }

    private fun isUserApp(
        applicationInfo: ApplicationInfo
    ): Boolean {

        return (
                applicationInfo.flags and
                        ApplicationInfo.FLAG_SYSTEM
                ) == 0
    }
}