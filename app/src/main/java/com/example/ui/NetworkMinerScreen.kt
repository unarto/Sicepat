package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Imports from modular subpackages
import com.example.ui.PCAPAnalysisTab
import com.example.ui.CyberSecurityTab
import com.example.ui.ComplianceAuditTab

@Composable
fun NetworkMinerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("network_miner_settings", Context.MODE_PRIVATE) }
    var isMinerEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("miner_enabled", true)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("PCAP & Analisis", "Keamanan Siber", "Audit Kepatuhan")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        ApplicationHeader(
            title = "Network Miner",
            onBack = onBack,
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (isMinerEnabled) "AKTIF" else "MATI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMinerEnabled) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = isMinerEnabled,
                        onCheckedChange = { checked ->
                            isMinerEnabled = checked
                            sharedPrefs.edit().putBoolean("miner_enabled", checked).apply()
                            Toast.makeText(
                                context,
                                if (checked) "Network Miner Diaktifkan" else "Network Miner Dinonaktifkan (Hemat Sumber Daya & VPN Optimal)",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4CAF50),
                            checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        )

        if (!isMinerEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFE53935).copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Miner Off",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Network Miner Dinonaktifkan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Semua aktivitas log parsing, capture PCAP, dan deteksi ancaman dihentikan sepenuhnya untuk menghemat daya baterai dan memaksimalkan bandwidth terowongan VPN.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        isMinerEnabled = true
                        sharedPrefs.edit().putBoolean("miner_enabled", true).apply()
                        Toast.makeText(context, "Network Miner Diaktifkan", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aktifkan Sekarang",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F141C),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else Color.Gray
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> PCAPAnalysisTab()
                    1 -> CyberSecurityTab()
                    2 -> ComplianceAuditTab()
                }
            }
        }
    }
}
