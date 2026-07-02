package com.example.ui

import com.example.viewmodel.AppViewModel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(val name: String, val packageName: String, var isChecked: Boolean = false)

@Composable
fun AppAccessControlScreen(
    viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appsList = remember { mutableStateListOf<AppItem>() }
    var isLoading by remember { mutableStateOf(true) }

    // Helper function to save the excluded packages to SharedPreferences
    val saveExcludedApps = { list: List<AppItem> ->
        val excluded = list.filter { it.isChecked }.map { it.packageName }.toSet()
        context.getSharedPreferences("vpn_split_tunnel_settings", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("excluded_packages", excluded)
            .apply()
    }

    // Load installed launcher applications asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableApps = pm.queryIntentActivities(mainIntent, 0)
            val sharedPrefs = context.getSharedPreferences("vpn_split_tunnel_settings", Context.MODE_PRIVATE)
            val selectedPackages = sharedPrefs.getStringSet("excluded_packages", emptySet()) ?: emptySet()

            var temp = launchableApps.mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val name = resolveInfo.loadLabel(pm).toString()
                AppItem(
                    name = name,
                    packageName = packageName,
                    isChecked = selectedPackages.contains(packageName)
                )
            }.distinctBy { it.packageName }

            // Fallback if launcher query returned empty (which can happen under some sandbox environments)
            if (temp.isEmpty()) {
                try {
                    val packages = pm.getInstalledPackages(0)
                    temp = packages.mapNotNull { packageInfo ->
                        val packageName = packageInfo.packageName
                        if (packageName == context.packageName) return@mapNotNull null
                        val name = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
                        AppItem(
                            name = name,
                            packageName = packageName,
                            isChecked = selectedPackages.contains(packageName)
                        )
                    }
                } catch (e: Exception) {
                    Log.e("AppAccessControlScreen", "Error loading package fallback", e)
                }
            }

            val sortedTemp = temp.sortedBy { it.name.lowercase() }

            withContext(Dispatchers.Main) {
                appsList.clear()
                appsList.addAll(sortedTemp)
                isLoading = false
            }
        }
    }

    val selectedCount = appsList.count { it.isChecked }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ApplicationHeader(
                title = "App access control", 
                onBack = onBack,
                actions = {
                    IconButton(onClick = { /* menu action */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )

            // Dynamic excluded card info matching screenshot
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2025))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "The selected application will be excluded from VPN",
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3748)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Selected $selectedCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF03A9F4))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(appsList.size, key = { index -> appsList[index].packageName }) { index ->
                        val app = appsList[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    val updatedApp = app.copy(isChecked = !app.isChecked)
                                    appsList[index] = updatedApp
                                    saveExcludedApps(appsList)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF2D3748), shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.name.take(1), 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = app.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = app.packageName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Checkbox(
                                checked = app.isChecked,
                                onCheckedChange = { isChecked ->
                                    appsList[index] = app.copy(isChecked = isChecked)
                                    saveExcludedApps(appsList)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF03A9F4),
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    }
                }
            }
        }

        // FLOATING SELECT ALL BUTTON (pill at bottom right)
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val allChecked = appsList.all { it.isChecked }
                        for (i in appsList.indices) {
                            appsList[i] = appsList[i].copy(isChecked = !allChecked)
                        }
                        saveExcludedApps(appsList)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Select all", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
