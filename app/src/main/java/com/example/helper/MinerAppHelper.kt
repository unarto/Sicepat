package com.example.helper

import android.content.Context
import android.widget.Toast
import com.example.dto.MinerAppItem

fun getInstalledLauncherApps(context: Context): List<MinerAppItem> {
    val pm = context.packageManager
    val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val launchableApps = pm.queryIntentActivities(mainIntent, 0)
    val sharedPrefs = context.getSharedPreferences("vpn_split_tunnel_settings", Context.MODE_PRIVATE)
    val selectedPackages = sharedPrefs.getStringSet("excluded_packages", emptySet()) ?: emptySet()

    return launchableApps.mapNotNull { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        if (packageName == context.packageName) return@mapNotNull null
        val name = resolveInfo.loadLabel(pm).toString()
        MinerAppItem(
            name = name,
            packageName = packageName,
            isExcluded = selectedPackages.contains(packageName)
        )
    }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
}

fun toggleAppVpnExclusion(context: Context, packageName: String, currentExcluded: Boolean, onUpdated: () -> Unit) {
    val sharedPrefs = context.getSharedPreferences("vpn_split_tunnel_settings", Context.MODE_PRIVATE)
    val excluded = sharedPrefs.getStringSet("excluded_packages", emptySet())?.toMutableSet() ?: mutableSetOf()
    if (currentExcluded) {
        excluded.remove(packageName)
        Toast.makeText(context, "Akses: Diatur lewat VPN", Toast.LENGTH_SHORT).show()
    } else {
        excluded.add(packageName)
        Toast.makeText(context, "Akses: Dikecualikan dari VPN (Bypass)", Toast.LENGTH_SHORT).show()
    }
    sharedPrefs.edit().putStringSet("excluded_packages", excluded).apply()
    onUpdated()
}
