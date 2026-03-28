
package dev.secam.simpletag.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** returns versionName as defined in build.gradle
 *
 */
fun getAppVersion(
    context: Context,
): String? {
    return try {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
        packageInfo.versionName

    } catch (_: Exception) {
        null
    }
}