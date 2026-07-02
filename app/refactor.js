const fs = require('fs');

const path = 'app/src/main/java/com/example/ui/ProxiesScreen.kt';
let code = fs.readFileSync(path, 'utf8');

const types = [
    { type: 'vmess', dialog: 'AddVmessDialog' },
    { type: 'vless', dialog: 'AddVlessDialog' },
    { type: 'trojan', dialog: 'AddTrojanDialog' },
    { type: 'histeria2', dialog: 'AddHysteria2Dialog' },
    { type: 'shadowsocks', dialog: 'AddShadowsocksDialog' },
    { type: 'wireguard', dialog: 'AddWireGuardDialog' },
    { type: 'socks', dialog: 'AddSocksDialog' },
    { type: 'http', dialog: 'AddHttpDialog' },
];

let buildConfigFromView = `
fun buildConfigFromView(type: String, view: android.view.View, name: String): String {
    return when(type) {
`;

for (const t of types) {
    const regex = new RegExp(`fun ${t.dialog}\\([\\s\\S]*?onClick = \\{([\\s\\S]*?)onSave\\(name, ([^\\)]+)\\)\\s*\\}\\s*else[\\s\\S]*?\\}\\s*\\)\\s*\\}`, 'm');
    const match = code.match(regex);
    if (match) {
        let logic = match[1];
        // Remove 'val view = rawView' and 'if (view != null) {'
        logic = logic.replace(/val view = rawView[\s\S]*?if\s*\(view != null\)\s*\{/, '');
        // Remove 'val etRemarks = ...' and 'val name = ...'
        logic = logic.replace(/val etRemarks = view\.findViewById<EditText>\(R\.id\.et_remarks\)[\s\S]*?val name =.*?Server"/, '');
        
        // Remove extra spaces
        logic = logic.trim();
        
        let outputVar = match[2].trim();
        
        buildConfigFromView += `        "${t.type}", "${t.type.toLowerCase()}", "${t.dialog.replace('Add', '').replace('Dialog', '').toLowerCase()}" -> {\n            ${logic}\n            ${outputVar}\n        }\n`;
        
        // Now remove the dialog from the code
        code = code.replace(new RegExp(`@Composable\\s*fun ${t.dialog}\\([\\s\\S]*?^\\}`, 'm'), '');
    }
}

buildConfigFromView += `        else -> ""\n    }\n}\n`;

code += "\n\n" + buildConfigFromView;

// Now modify AddProxyScreen
const addProxyScreenOld = `                OutlinedTextField(
                    value = fullConfigText,
                    onValueChange = { fullConfigText = it },
                    label = { Text("Config URI (e.g. \${activeAddType}://...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val name = if (remarksText.isNotBlank()) remarksText else "New $displayType Server"
                            proxiesList.add(0, ProxyItem(name, displayType, "Waiting", false, fullConfig = fullConfigText, profileName = currentProfileName))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simpan", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }`;

const addProxyScreenNew = `                val layoutRes = when (activeAddType.lowercase()) {
                    "vmess" -> R.layout.layout_vmess
                    "vless" -> R.layout.layout_vless
                    "shadowsocks" -> R.layout.layout_shadowsocks
                    "trojan" -> R.layout.layout_trojan
                    "histeria2", "hysteria" -> R.layout.layout_hysteria2
                    "wireguard" -> R.layout.layout_wireguard
                    "socks", "socks5" -> R.layout.layout_socks
                    "http" -> R.layout.layout_http
                    else -> R.layout.layout_vmess
                }

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            val view = LayoutInflater.from(context).inflate(layoutRes, null, false)
                            setupTlsVisibility(view)
                            rawView = view
                            view
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val view = rawView
                            if (view != null) {
                                val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                                val remarksExtracted = etRemarks?.text?.toString() ?: ""
                                val name = if (remarksExtracted.isNotBlank()) remarksExtracted else "New $displayType Server"
                                val configStr = buildConfigFromView(activeAddType.lowercase(), view, name)
                                proxiesList.add(0, ProxyItem(name, displayType, "Waiting", false, fullConfig = configStr, profileName = currentProfileName))
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simpan", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }`;

code = code.replace(addProxyScreenOld, addProxyScreenNew);

// Add rawView state to AddProxyScreen
const rawViewState = `    var fullConfigText by remember { mutableStateOf("") }
    var rawView by remember { mutableStateOf<android.view.View?>(null) }`;
code = code.replace(`    var fullConfigText by remember { mutableStateOf("") }`, rawViewState);

// Revert onAddTypeClick
code = code.replace(/onAddTypeClick = \{ type ->[\s\S]*?\},/, `onAddTypeClick = { type ->
                        activeAddType = type
                    },`);

// Remove showVmessDialog state etc.
const statesToRemove = [
    `var showWireGuardDialog by remember { mutableStateOf(false) }\n`,
    `var showHysteria2Dialog by remember { mutableStateOf(false) }\n`,
    `var showVmessDialog by remember { mutableStateOf(false) }\n`,
    `var showVlessDialog by remember { mutableStateOf(false) }\n`,
    `var showTrojanDialog by remember { mutableStateOf(false) }\n`,
    `var showShadowsocksDialog by remember { mutableStateOf(false) }\n`,
    `var showSocksDialog by remember { mutableStateOf(false) }\n`,
    `var showHttpDialog by remember { mutableStateOf(false) }\n`
];

for (const state of statesToRemove) {
    code = code.replace(state, '');
}

// Remove the dialog usages
code = code.replace(/if \(showHysteria2Dialog\) \{[\s\S]*?showHttpDialog = false\n                \}\n            \)\n        \}/, '');

fs.writeFileSync(path, code);
console.log('Done refactoring proxies screen');
