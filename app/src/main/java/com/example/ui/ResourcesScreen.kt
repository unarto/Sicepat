package com.example.ui

import com.example.viewmodel.AppViewModel
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class ResourceItem(val name: String, val meta: String, val url: String)

@Composable
fun ResourcesScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf<String?>(null) }

    val resources = listOf(
        ResourceItem("GEOIP", "v2ray-rules-dat", "https://github.com/unarto/geoip/releases/download/202606250051/geoip.dat"),
        ResourceItem("GEOSITE", "v2ray-rules-dat", "https://github.com/unarto/geoip/releases/download/202606250051/geosite.dat")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationHeader(title = "Resources", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(resources) { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    border = BorderStroke(1.dp, Color(0xFF2E3238))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = res.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = res.meta, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = res.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF2E3238))
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isSyncing == res.name) return@OutlinedButton
                                    coroutineScope.launch {
                                        isSyncing = res.name
                                        val fileName = if (res.name == "GEOIP") "geoip.dat" else "geosite.dat"
                                        val success = viewModel.downloadResource(res.url, fileName, context)
                                        isSyncing = null
                                        if (success) {
                                            Toast.makeText(context, "${res.name} updated successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to update ${res.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF2E3238))
                            ) {
                                if (isSyncing == res.name) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSyncing == res.name) "Syncing..." else "Sync", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
