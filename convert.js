const fs = require('fs');

const file = 'app/src/main/java/com/example/ProxiesScreen.kt';
let code = fs.readFileSync(file, 'utf-8');

const regex = /Card\(\s*modifier = Modifier[\s\S]*?CardDefaults\.cardColors\(containerColor = containerColor\)\n    \) \{[\s\S]*?Row\([\s\S]*?\},[\s\S]*?\}\n        \}\n    \}/;

const newProxyCardContent = `    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, outlineColor, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        if (isGrid) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = proxy.name,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Box {
                        IconButton(onClick = { showShareMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false },
                            modifier = Modifier.background(Color(0xFF1E2228))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit", color = Color.White) },
                                onClick = { showShareMenu = false; showEditDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.White) },
                                onClick = { showShareMenu = false; showDeleteDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Config", color = Color.White) },
                                onClick = { 
                                    showShareMenu = false
                                    clipboardManager.setText(AnnotatedString("clash://install-config?url=https://myserver.com/xray-import?name=\${proxy.name}"))
                                    Toast.makeText(context, "Exported proxy!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacingBetween))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = proxy.type, fontSize = metaSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = proxy.latency, 
                        fontSize = metaSize, 
                        fontWeight = FontWeight.Medium,
                        color = if (proxy.isGreen) Color(0xFF4CAF50) else if (proxy.latency == "Timeout") Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proxy.name,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(spacingBetween))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = proxy.type, fontSize = metaSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = proxy.latency, 
                            fontSize = metaSize, 
                            fontWeight = FontWeight.Medium,
                            color = if (proxy.isGreen) Color(0xFF4CAF50) else if (proxy.latency == "Timeout") Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false },
                            modifier = Modifier.background(Color(0xFF1E2228))
                        ) {
                            DropdownMenuItem(
                                text = { Text("qrcode", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showShareMenu = false
                                    showQrDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("export from clipboard", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showShareMenu = false
                                    clipboardManager.setText(AnnotatedString("clash://install-config?url=https://myserver.com/xray-import?name=\${proxy.name}"))
                                    Toast.makeText(context, "Exported config based on clipboard format to system memory!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Tulis / Edit icon
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Hapus / Delete icon
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }`;

code = code.replace(regex, newProxyCardContent);
fs.writeFileSync(file, code, 'utf-8');
console.log("Replaced using regex!");
