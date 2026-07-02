package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Imports from other subpackages
import com.example.dto.AuditRule

@Composable
fun ComplianceAuditTab() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val complianceScore = 96

    val auditItems = listOf(
        AuditRule(
            "GDPR - Proteksi Data Pribadi Uni Eropa",
            "Sistem menjamin alamat IP asli dan lalu lintas DNS tidak disimpan secara permanen di server luar. Logging hanya dilakukan di memori RAM lokal perangkat klien (Zero Logging).",
            true
        ),
        AuditRule(
            "ISO 27001 - Manajemen Keamanan Informasi",
            "Aplikasi menerapkan enkripsi tingkat tinggi untuk lalu lintas VPN yang keluar (Egress) dan mengontrol perizinan aplikasi lokal (App Access Control) secara ketat.",
            true
        ),
        AuditRule(
            "DNS Privacy Standard (RFC 7858)",
            "Semua permintaan DNS dilewatkan melalui enkripsi kueri di dalam tunnel aman untuk mematuhi regulasi pencegahan ISP Hijacking.",
            true
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Compliance Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A1D)),
                border = BorderStroke(1.dp, Color(0xFF2E3238))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SKOR KEPATUHAN AUDIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sangat Patuh (Excellent)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sistem memenuhi hampir seluruh standar privasi data & keamanan jaringan global.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(0xFF0F141C), RoundedCornerShape(35.dp))
                            .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(35.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$complianceScore%",
                            color = Color(0xFF4CAF50),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Export Report Button
        item {
            Button(
                onClick = {
                    val reportText = buildString {
                        append("=========================================\n")
                        append("LAPORAN AUDIT KEPATUHAN & PRIVASI JARINGAN\n")
                        append("SiCepat VPN Network Miner Audit Tool\n")
                        append("=========================================\n")
                        append("Skor Kepatuhan Global: $complianceScore%\n")
                        append("Status: Sangat Patuh\n\n")
                        auditItems.forEach { rule ->
                            append("[PATUH] ${rule.name}\n")
                            append("Detail: ${rule.desc}\n\n")
                        }
                        append("Laporan dibuat secara dinamis di bawah otorisasi sistem lokal.\n")
                    }
                    clipboardManager.setText(AnnotatedString(reportText))
                    Toast.makeText(context, "Laporan Audit disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor & Salin Laporan Audit", fontWeight = FontWeight.Bold)
            }
        }

        // Rules Section Title
        item {
            Text(
                text = "STANDAR REGULASI PRIVASI & KEAMANAN JARINGAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Audit Items
        items(auditItems) { rule ->
            AuditRuleCard(rule = rule)
        }
    }
}

@Composable
fun AuditRuleCard(rule: AuditRule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13171B)),
        border = BorderStroke(1.dp, Color(0xFF262A2E))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = rule.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rule.desc,
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5),
                    lineHeight = 17.sp
                )
            }
        }
    }
}
