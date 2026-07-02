package com.example.ui

import com.example.viewmodel.AppViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(viewModel: AppViewModel) {
    var showAddProfile by remember { mutableStateOf(false) }
    var editingProfileIndex by remember { mutableStateOf<Int?>(null) }
    var showUrlDialog by remember { mutableStateOf(false) }
    
    val profiles = viewModel.profiles
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloadingUrl by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.use { r -> r.readText() } ?: ""
                if (content.isNotBlank()) {
                    var fileName = "File_Impor.json"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }

                    val tempProxyList = viewModel.parseSubscriptionContent(content, fileName)
                    if (tempProxyList.isNotEmpty()) {
                        viewModel.addProfileWithProxies(
                            name = fileName,
                            url = "",
                            source = "File Import",
                            customProxies = tempProxyList
                        )
                        Toast.makeText(context, "Profil berhasil diimpor dari file!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Format file tidak valid atau kosong.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val editingIndex = editingProfileIndex
    if (editingIndex != null) {
        val profileItem = profiles[editingIndex]
        EditProfileScreen(
            profile = profileItem,
            onBack = { editingProfileIndex = null },
            onSave = { updated ->
                viewModel.updateProfile(editingIndex, updated)
                editingProfileIndex = null
            },
            onDelete = {
                viewModel.removeProfile(editingIndex)
                editingProfileIndex = null
            }
        )
    } else {
        var isUpdating by remember { mutableStateOf(false) }

        val infiniteTransition = rememberInfiniteTransition(label = "rotation_transition")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation_angle"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                 // Header
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        IconButton(
                            onClick = {
                                if (!isUpdating) {
                                    isUpdating = true
                                    Toast.makeText(context, "Memperbarui semua profil...", Toast.LENGTH_SHORT).show()
                                    viewModel.updateAllProfiles { resultSummary ->
                                        isUpdating = false
                                        Toast.makeText(context, resultSummary, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.rotate(if (isUpdating) rotationAngle else 0f)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.sortProfilesAlphabetically()
                                Toast.makeText(context, "Profil diurutkan berdasarkan abjad (A-Z)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Sort A-Z",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(profiles.size, key = { index -> "${profiles[index].name}_$index" }) { index ->
                        val profile = profiles[index]
                        ProfileCard(
                            profile = profile,
                            onSelect = {
                                viewModel.selectProfile(index)
                            },
                            onEdit = {
                                editingProfileIndex = index
                            },
                            onDelete = {
                                viewModel.removeProfile(index)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            // FAB 
            FloatingActionButton(
                onClick = { showAddProfile = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF03A9F4),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        if (showAddProfile) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showAddProfile = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("QR code") },
                        supportingContent = { Text("Scan QR code to obtain profile") },
                        leadingContent = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        modifier = Modifier.clickable { 
                            showAddProfile = false
                            Toast.makeText(context, "Scan QR Code is not yet implemented", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("File") },
                        supportingContent = { Text("Directly upload profile") },
                        leadingContent = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                        modifier = Modifier.clickable { 
                            showAddProfile = false
                            filePickerLauncher.launch("*/*")
                        }
                    )
                    ListItem(
                        headlineContent = { Text("URL") },
                        supportingContent = { Text("Obtain profile through URL") },
                        leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                        modifier = Modifier.clickable { 
                            showAddProfile = false
                            showUrlDialog = true
                        }
                    )
                }
            }
        }

        if (showUrlDialog) {
            var urlInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = { Text("Import from URL") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL") },
                            isError = urlInput.isBlank(),
                            supportingText = { if (urlInput.isBlank()) Text("cannot be empty", color = MaterialTheme.colorScheme.error) }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                isDownloadingUrl = true
                                coroutineScope.launch {
                                    val result = viewModel.downloadConfigFromUrl(urlInput)
                                    if (result != null) {
                                        val destinationName = "URL Profile ${System.currentTimeMillis() / 1000}"
                                        val parsedProxies = viewModel.parseConfigToProxies(result, destinationName)
                                        if (parsedProxies.isNotEmpty()) {
                                            viewModel.addProfileWithProxies(
                                                name = destinationName,
                                                url = urlInput,
                                                source = "URL Download",
                                                customProxies = parsedProxies
                                            )
                                            Toast.makeText(context, "Berhasil mengimpor: ${parsedProxies.size} Server!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Berhasil mengunduh tapi gagal parsing proxy.", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Gagal mengunduh atau data kosong.", Toast.LENGTH_LONG).show()
                                    }
                                    isDownloadingUrl = false
                                    showUrlDialog = false
                                }
                            }
                        }
                    ) {
                        if (isDownloadingUrl) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Submit")
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun AddProfileScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.use { r -> r.readText() } ?: ""
                if (content.isNotBlank()) {
                    var fileName = "File_Impor.json"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }

                    val tempProxyList = viewModel.parseSubscriptionContent(content, fileName)
                    if (tempProxyList.isNotEmpty()) {
                        viewModel.addProfileWithProxies(
                            name = fileName,
                            url = "",
                            source = "File Import",
                            customProxies = tempProxyList
                        )
                        Toast.makeText(context, "Profil berhasil diimpor dari file!", Toast.LENGTH_SHORT).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Format file tidak valid atau kosong.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var urlProfileName by remember { mutableStateOf("") }
    var urlLinkInput by remember { mutableStateOf("") }
    var isDownloadingUrl by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1218))
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Konfigurasi Profil Baru",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Custom Tab Row
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                listOf(
                    Pair("File/Manual", Icons.Default.FolderOpen),
                    Pair("Unduhan URL", Icons.Default.CloudDownload)
                ).forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Button(
                        onClick = { selectedTab = index },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) Color.White else Color(0xFF90A4AE)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = tab.second, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = tab.first, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Content panel based on Tab
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                // Tab 0: FILE IMPORT & MANUAL PASTE
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Impor Konfigurasi (.json, .txt, .conf)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )

                        // Real File Chooser Button
                        Button(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PILIH FILE KONFIGURASI (.json / .txt)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Atau Tempel Tautan Konfigurasi (vmess/vless/ss/trojan):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF90A4AE)
                        )
                        
                        var manualPasteText by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = manualPasteText,
                            onValueChange = { manualPasteText = it },
                            placeholder = { Text("Paste config link here...", color = Color(0xFF546E7A)) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color(0xFF233545)
                            )
                        )
                        
                        Button(
                            onClick = {
                                if (manualPasteText.isNotBlank()) {
                                    val parsedProxies = viewModel.parseSubscriptionContent(manualPasteText, "Manual Import")
                                    if (parsedProxies.isNotEmpty()) {
                                        viewModel.addProfileWithProxies(
                                            name = "Manual Import ${System.currentTimeMillis() / 1000}",
                                            source = "Manual",
                                            customProxies = parsedProxies
                                        )
                                        Toast.makeText(context, "Berhasil mengimpor ${parsedProxies.size} server!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Format tidak valid.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = manualPasteText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("IMPOR DARI TAUTAN", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Pastikan file konfigurasi memiliki format yang valid untuk Xray/V2Ray.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: URL DOWNLOAD (URL SYNC VIA HTTP)
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Konfigurasi URL Download Subskripsi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )

                        OutlinedTextField(
                            value = urlProfileName,
                            onValueChange = { urlProfileName = it },
                            label = { Text("Nama Profil (Opsional)", color = Color(0xFF90CAF9)) },
                            placeholder = { Text("e.g. My Premium Config", color = Color(0xFF546E7A)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF1E88E5),
                                unfocusedBorderColor = Color(0xFF233545)
                            )
                        )

                        OutlinedTextField(
                            value = urlLinkInput,
                            onValueChange = { urlLinkInput = it },
                            label = { Text("URL Subskripsi", color = Color(0xFF90CAF9)) },
                            placeholder = { Text("https://example.com/config.json", color = Color(0xFF546E7A)) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF1E88E5),
                                unfocusedBorderColor = Color(0xFF233545)
                            )
                        )

                        Button(
                            onClick = {
                                if (urlLinkInput.isNotBlank()) {
                                    isDownloadingUrl = true
                                    coroutineScope.launch {
                                        val result = viewModel.downloadConfigFromUrl(urlLinkInput)
                                        if (result != null) {
                                            val destinationName = urlProfileName.ifBlank { "URL Profile ${System.currentTimeMillis() / 1000}" }
                                            val parsedProxies = viewModel.parseConfigToProxies(result, destinationName)
                                            if (parsedProxies.isNotEmpty()) {
                                                 viewModel.addProfileWithProxies(
                                                     name = destinationName,
                                                     url = urlLinkInput,
                                                     source = "URL Download",
                                                     customProxies = parsedProxies
                                                 )
                                                 isDownloadingUrl = false
                                                 Toast.makeText(context, "Berhasil mengimpor: ${parsedProxies.size} Server!", Toast.LENGTH_LONG).show()
                                                 onBack()
                                             } else {
                                                 isDownloadingUrl = false
                                                 Toast.makeText(context, "Berhasil mengunduh tapi gagal parsing proxy.", Toast.LENGTH_LONG).show()
                                             }
                                        } else {
                                            isDownloadingUrl = false
                                            Toast.makeText(context, "Gagal mengunduh atau data kosong.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Harap masukkan URL Subskripsi terlebih dahulu", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isDownloadingUrl
                        ) {
                            if (isDownloadingUrl) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sedang Menerima...")
                            } else {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sinkronkan & Impor URL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ProfileItem(
    val name: String,
    val date: String,
    val isSelected: Boolean,
    val optionalUrl: String = "",
    val userAgent: String = "",
    val remarksFilter: String = "",
    val enableUpdate: Boolean = true,
    val enableAutoUpdate: Boolean = false,
    val allowInsecureHttp: Boolean = false
)

@Composable
fun ProfileCard(
    profile: ProfileItem,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (profile.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.date,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: ProfileItem,
    onBack: () -> Unit,
    onSave: (ProfileItem) -> Unit,
    onDelete: () -> Unit
) {
    var remarks by remember { mutableStateOf(profile.name) }
    var optionalUrl by remember { mutableStateOf(profile.optionalUrl) }
    var userAgent by remember { mutableStateOf(profile.userAgent) }
    var remarksFilter by remember { mutableStateOf(profile.remarksFilter) }
    var enableUpdate by remember { mutableStateOf(profile.enableUpdate) }
    var enableAutoUpdate by remember { mutableStateOf(profile.enableAutoUpdate) }
    var allowInsecureHttp by remember { mutableStateOf(profile.allowInsecureHttp) }
    var previousRemarks by remember { mutableStateOf("The config remarks exist and are unique") }
    var nextRemarks by remember { mutableStateOf("") }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete Profile?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete profile '${profile.name}'?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = {
                        onSave(
                            profile.copy(
                                name = remarks,
                                optionalUrl = optionalUrl,
                                userAgent = userAgent,
                                remarksFilter = remarksFilter,
                                enableUpdate = enableUpdate,
                                enableAutoUpdate = enableAutoUpdate,
                                allowInsecureHttp = allowInsecureHttp
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Subscription group setting",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            // remarks field
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("remarks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Optional URL field
            OutlinedTextField(
                value = optionalUrl,
                onValueChange = { optionalUrl = it },
                label = { Text("Optional URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // User Agent field
            OutlinedTextField(
                value = userAgent,
                onValueChange = { userAgent = it },
                label = { Text("User Agent") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Remarks regular filter field
            OutlinedTextField(
                value = remarksFilter,
                onValueChange = { remarksFilter = it },
                label = { Text("Remarks regular filter") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Switches list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable update",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = enableUpdate,
                        onCheckedChange = { enableUpdate = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable automatic update",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = enableAutoUpdate,
                        onCheckedChange = { enableAutoUpdate = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Allow insecure HTTP address",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = allowInsecureHttp,
                        onCheckedChange = { allowInsecureHttp = it }
                    )
                }
            }

            // Previous proxy config remarks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Previous proxy config remarks",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = previousRemarks,
                    onValueChange = { previousRemarks = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Next proxy config remarks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Next proxy config remarks",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nextRemarks,
                    onValueChange = { nextRemarks = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
