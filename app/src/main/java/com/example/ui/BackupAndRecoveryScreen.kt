package com.example.ui

import com.example.viewmodel.AppViewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun BackupAndRecoveryScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var showWebdavDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showStrategyDialog by remember { mutableStateOf(false) }
    var showLocalBackupConfirm by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val outputStream = context.contentResolver.openOutputStream(uri)
            val success = viewModel.performExport(outputStream)
            if (success) {
                Toast.makeText(context, "Backup successfully written to local file storage!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Backup failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val success = viewModel.performImport(inputStream, viewModel.recoveryStrategy)
            if (success) {
                Toast.makeText(context, "Recovery finished successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Recovery failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val themeMode = viewModel.selectedThemeMode
    val isLightTheme = themeMode == "Light" || (themeMode == "Auto" && !isSystemInDarkTheme())

    val backgroundBrush = if (isLightTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFFFF),
                Color(0xFF87CEEB)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF14171E),
                Color(0xFF14171E)
            )
        )
    }

    val contentTextColor = if (isLightTheme) Color.Black else Color.White
    val contentSubtextColor = if (isLightTheme) Color.Gray else Color.LightGray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ApplicationHeader(title = "Backup and Recovery", onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Section: Remote
                item { SectionLabel("Remote", isLightTheme) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "WebDAV Status",
                            tint = if (viewModel.webdavIsBound) Color(0xFF90CAF9) else contentSubtextColor,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (viewModel.webdavIsBound) viewModel.webdavAccount else "No info",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (viewModel.webdavIsBound) "WebDAV: " + viewModel.webdavAddress else "Please bind WebDAV",
                                fontSize = 13.sp,
                                color = contentSubtextColor
                            )
                        }
                        Button(
                            onClick = { showWebdavDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLightTheme) Color(0xFFE3F2FD) else Color(0xFF1E3A5F)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (viewModel.webdavIsBound) "Configure" else "Bind",
                                color = if (isLightTheme) Color(0xFF1565C0) else Color(0xFF81D4FA),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (viewModel.webdavIsBound) {
                    item {
                        var isSyncing by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSyncing) {
                                    isSyncing = true
                                    viewModel.syncBackupToWebdav(context) { success, msg ->
                                        isSyncing = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSyncing) "Syncing Backup..." else "Upload Backup to WebDAV",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentTextColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upload local config and profiles to WebDAV",
                                    fontSize = 13.sp,
                                    color = contentSubtextColor
                                )
                            }
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF90CAF9),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    item {
                        var isRestoring by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isRestoring) {
                                    isRestoring = true
                                    viewModel.restoreBackupFromWebdav(context) { success, msg ->
                                        isRestoring = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRestoring) "Restoring..." else "Restore Backup from WebDAV",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentTextColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Restore all configs and profiles from WebDAV",
                                    fontSize = 13.sp,
                                    color = contentSubtextColor
                                )
                            }
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF90CAF9),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Section: Local
                item { SectionLabel("Local", isLightTheme) }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLocalBackupConfirm = true }
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Backup",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Backup local data to local",
                            fontSize = 13.sp,
                            color = contentSubtextColor
                        )
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecoveryDialog = true }
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Recovery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recovery data from file",
                            fontSize = 13.sp,
                            color = contentSubtextColor
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Section: Options
                item { SectionLabel("Options", isLightTheme) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStrategyDialog = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recovery strategy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose conflict resolution behavior",
                                fontSize = 13.sp,
                                color = contentSubtextColor
                            )
                        }
                        Button(
                            onClick = { showStrategyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLightTheme) Color(0xFFBBDEFB) else Color(0xFF22425E)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = viewModel.recoveryStrategy,
                                color = if (isLightTheme) Color(0xFF0D47A1) else Color(0xFF90CAF9),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 1. WebDAV Configuration Dialog
        if (showWebdavDialog) {
            WebDavConfigDialog(
                viewModel = viewModel,
                onDismiss = { showWebdavDialog = false },
                onSave = { address, account, password ->
                    viewModel.bindWebdav(context, address, account, password)
                    showWebdavDialog = false
                    Toast.makeText(context, "WebDAV config saved and synced!", Toast.LENGTH_SHORT).show()
                },
                onUnbind = if (viewModel.webdavIsBound) {
                    {
                        viewModel.unbindWebdav(context)
                        showWebdavDialog = false
                        Toast.makeText(context, "WebDAV un-bound safely.", Toast.LENGTH_SHORT).show()
                    }
                } else null
            )
        }

        // 2. Local Backup Confirm Dialog
        if (showLocalBackupConfirm) {
            AlertDialog(
                onDismissRequest = { showLocalBackupConfirm = false },
                title = { Text("Backup Local Data") },
                text = { Text("Do you want to write all active profiles, proxy configurations, and app routes into a secure local backup snapshot?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLocalBackupConfirm = false
                        createDocumentLauncher.launch("sicepatxray_backup.json")
                    }) {
                        Text("Backup Now", color = Color(0xFF90CAF9))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocalBackupConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. Recovery Options Dialog
        if (showRecoveryDialog) {
            RecoverySelectionDialog(
                onDismiss = { showRecoveryDialog = false },
                onSelect = { option ->
                    showRecoveryDialog = false
                    openDocumentLauncher.launch(arrayOf("application/json"))
                }
            )
        }

        // 4. Recovery Strategy Dialog
        if (showStrategyDialog) {
            StrategySelectionDialog(
                selectedStrategy = viewModel.recoveryStrategy,
                onDismiss = { showStrategyDialog = false },
                onSelect = { strategy ->
                    viewModel.updateRecoveryStrategy(context, strategy)
                    showStrategyDialog = false
                    Toast.makeText(context, "Recovery strategy set to: $strategy", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, isLightTheme: Boolean) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isLightTheme) Color(0xFF1565C0) else Color(0xFF90CAF9),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun RecoverySelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Recovery",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onSelect("Profiles") },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Only recovery profiles",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }

                    TextButton(
                        onClick = { onSelect("AllData") },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Recovery all data",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StrategySelectionDialog(
    selectedStrategy: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Recovery strategy",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))

                listOf("Compatible", "Override").forEach { strategy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(strategy) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (strategy == selectedStrategy),
                            onClick = { onSelect(strategy) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF90CAF9),
                                unselectedColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = strategy,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WebDavConfigDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onUnbind: (() -> Unit)? = null
) {
    var address by remember { mutableStateOf(viewModel.webdavAddress) }
    var account by remember { mutableStateOf(viewModel.webdavAccount) }
    var password by remember { mutableStateOf(viewModel.webdavPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "WebDAV configuration",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Address Field
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color.LightGray) },
                    supportingText = { Text("WebDAV server address", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF90CAF9),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF90CAF9),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Account Field
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Account") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF90CAF9),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF90CAF9),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null, tint = Color.LightGray) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.LightGray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF90CAF9),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF90CAF9),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onUnbind != null) {
                        TextButton(onClick = onUnbind) {
                            Text("Unbind", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSave(address, account, password) }) {
                        Text("Save", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
