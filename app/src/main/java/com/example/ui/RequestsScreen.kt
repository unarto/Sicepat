package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RequestRecord(val url: String, val meta: String, val category: String, val type: String)

@Composable
fun RequestsScreen(onBack: () -> Unit) {
    val requests = listOf(
        RequestRecord("tcp://raw.githubusercontent.com:443", "Just now · mihomo · 0B ↑ 0B ↓", "VLESS WC DigitalOcean, LLC", "Proxy"),
        RequestRecord("tcp://raw.githubusercontent.com:443", "11 minutes ago · mihomo · 0B ↑ 0B ↓", "VLESS WC DigitalOcean, LLC", "Proxy"),
        RequestRecord("tcp://raw.githubusercontent.com:443", "17 minutes ago · mihomo · 0B ↑ 0B ↓", "yu.xhmt.web.id", "Proxy"),
        RequestRecord("tcp://raw.githubusercontent.com:443", "19 minutes ago · mihomo · 0B ↑ 0B ↓", "yu.xhmt.web.id", "Proxy"),
        RequestRecord("tcp://raw.githubusercontent.com:443", "21 minutes ago · mihomo · 0B ↑ 0B ↓", "yu.xhmt.web.id", "Proxy")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ApplicationHeader(
                title = "Requests", 
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(requests) { req ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(text = req.url, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = req.meta, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E3238))
                            ) {
                                Text(
                                    text = req.category, 
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E3238))
                            ) {
                                Text(
                                    text = req.type, 
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                }
            }
        }

        FloatingActionButton(
            onClick = {},
            containerColor = Color(0xFF03A9F4),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Block, contentDescription = "Stop", tint = Color.White)
        }
    }
}
