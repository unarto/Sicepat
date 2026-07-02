package com.example.ui

import com.example.viewmodel.AppViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemeScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var selectedColorIndex by remember { mutableStateOf(2) } // Circle 3 selected
    var pureBlack by remember { mutableStateOf(false) }
    var textScaling by remember { mutableStateOf(false) }
    var sliderVal by remember { mutableStateOf(0.3f) }

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationHeader(title = "Theme", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Theme mode section
            item {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Theme mode", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Auto" to Icons.Default.Autorenew, "Light" to Icons.Default.LightMode, "Dark" to Icons.Default.DarkMode).forEach { (mode, icon) ->
                        val isSel = viewModel.selectedThemeMode == mode
                        Button(
                            onClick = { viewModel.updateThemeMode(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF1E3A5F) else Color(0xFF1C2025)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp, 
                                if (isSel) Color(0xFF81D4FA) else Color(0xFF2E3238)
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = icon, 
                                contentDescription = mode, 
                                tint = if (isSel) Color(0xFF81D4FA) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode, 
                                fontSize = 14.sp, 
                                color = if (isSel) Color(0xFF81D4FA) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Theme color section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Theme color", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Content", color = Color(0xFF0D47A1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Theme Color", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Quadrant circles items grid!
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val colors = listOf(
                        listOf(Color(0xFFFFD1D1), Color(0xFFFFE3E3), Color(0xFFFFD1D1), Color(0xFFFFE3E3)), // Pinkish
                        listOf(Color(0xFFFFCC80), Color(0xFFFFE0B2), Color(0xFFC8E6C9), Color(0xFFE8F5E9)), // Peach/Mint
                        listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE), Color(0xFFCE93D8), Color(0xFFF3E5F5)), // Custom Cyan/Purple (Selected!)
                        listOf(Color(0xFFFFFFFF), Color(0xFFEEEEEE), Color(0xFFD4E157), Color(0xFFF0F4C3)), // White/Lime
                        listOf(Color(0xFF9FA8DA), Color(0xFFE8EAF6), Color(0xFFCE93D8), Color(0xFFF3E5F5)), // Lavender/Pink
                        listOf(Color(0xFFA5D6A7), Color(0xFFE8F5E9), Color(0xFF80CBC4), Color(0xFFE0F2F1)), // Mint/Teal
                        listOf(Color(0xFFFFAB91), Color(0xFFFBE9E7), Color(0xFFFFE0B2), Color(0xFFFFF3E0)), // Coral/Beige
                        listOf(Color(0xFFB39DDB), Color(0xFFEDE7F6), Color(0xFFF48FB1), Color(0xFFFCE4EC))  // Purple/Pink
                    )

                    // Draw 4 circles per row
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..3) {
                                val index = row * 4 + col
                                if (index < colors.size) {
                                    val quads = colors[index]
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color(0xFF1E2228), CircleShape)
                                            .border(
                                                2.dp, 
                                                if (selectedColorIndex == index) Color(0xFF81D4FA) else Color.Transparent, 
                                                CircleShape
                                            )
                                            .clickable { selectedColorIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.size(46.dp)) {
                                            drawArc(quads[0], startAngle = 0f, sweepAngle = 90f, useCenter = true)
                                            drawArc(quads[1], startAngle = 90f, sweepAngle = 90f, useCenter = true)
                                            drawArc(quads[2], startAngle = 180f, sweepAngle = 90f, useCenter = true)
                                            drawArc(quads[3], startAngle = 270f, sweepAngle = 90f, useCenter = true)
                                        }

                                        if (selectedColorIndex == index) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(Color(0xBB0D47A1), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check, 
                                                    contentDescription = "Selected Theme Color", 
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Row Circle 9: add button '+'
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF1C2025), CircleShape)
                                .border(1.dp, Color(0xFF2E3238), CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom color", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Pure black mode
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Brightness2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Pure black mode", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(
                        checked = pureBlack,
                        onCheckedChange = { pureBlack = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = Color(0xFF2E3238),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // Text scaling mode
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Text Scaling", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(
                        checked = textScaling,
                        onCheckedChange = { textScaling = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = Color(0xFF2E3238),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // Slider scaling
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF03A9F4),
                            activeTrackColor = Color(0xFF03A9F4),
                            inactiveTrackColor = Color(0xFF2E3238)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${(100 + sliderVal * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
