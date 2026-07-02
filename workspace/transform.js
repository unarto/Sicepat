const fs = require('fs');

const file = 'app/src/main/java/com/example/ToolsScreen.kt';
let code = fs.readFileSync(file, 'utf-8');

// Replace the routing
code = code.replace(
    /"Advanced configuration" -> AdvancedConfigurationScreen\(onBack = \{ activeSubScreen = null \}\)/,
    ''
);

// Remove the Advanced configuration from Tools screen list
code = code.replace(
    /item \{ ToolListItem\(Icons\.Default\.Build, "Advanced configuration", "Provide diverse configuration options"\) \{ activeSubScreen = "Advanced configuration" \} \}/,
    ''
);

// We need to inject the Advanced Configuration settings into the "Settings" section block.
// We'll replace the old AdvancedConfigurationScreen composable with AdvancedConfigurationContent composable
// We start by finding `fun AdvancedConfigurationScreen(onBack: () -> Unit) {`
const startIdx = code.indexOf('@Composable\nfun AdvancedConfigurationScreen(onBack: () -> Unit) {');
const endIdx = code.indexOf('@Composable\nfun SettingsSectionHeader(title: String) {');

if (startIdx !== -1 && endIdx !== -1) {
    let advancedBlock = code.substring(startIdx, endIdx);
    
    // Change signature
    advancedBlock = advancedBlock.replace( // 1
        /fun AdvancedConfigurationScreen\(onBack: \(\) -> Unit\) \{/,
        'fun AdvancedConfigurationContent() {'
    );

    // Remove the background Box and Header Row. The Box starts around line: Box( modifier = Modifier.fillMaxSize().background(Brush.verticalGradient...
    const boxStartStr = 'Box(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(\n                Brush.verticalGradient(\n                    colors = listOf(\n                        Color(0xFFFFFFFF),\n                        Color(0xFFE1F5FE) // Soft baby blue gradient matching the screenshot!\n                    )\n                )\n            )\n    ) {';
    
    advancedBlock = advancedBlock.replace(boxStartStr, 'Column(modifier = Modifier.fillMaxWidth()) {');
    
    // Remove the inner Column and Header
    const innerColStr = 'Column(modifier = Modifier.fillMaxSize()) {\n            // Header exactly like screenshots ("Settings", dark blue onBack tint arrow)\n            Row(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .padding(horizontal = 8.dp, vertical = 12.dp),\n                verticalAlignment = Alignment.CenterVertically\n            ) {\n                IconButton(onClick = onBack) {\n                    Icon(\n                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,\n                        contentDescription = "Back",\n                        tint = Color(0xFF263238),\n                        modifier = Modifier.size(24.dp)\n                    )\n                }\n                Spacer(modifier = Modifier.width(12.dp))\n                Text(\n                    text = "Settings",\n                    fontSize = 21.sp,\n                    fontWeight = FontWeight.Bold,\n                    color = Color(0xFF263238)\n                )\n            }';
    
    advancedBlock = advancedBlock.replace(innerColStr, '');
    
    // Change LazyColumn to Column
    advancedBlock = advancedBlock.replace(
        /LazyColumn\(modifier = Modifier\.fillMaxSize\(\)\) \{/,
        'Column(modifier = Modifier.fillMaxWidth()) {'
    );

    // Remove all 'item {' and '}' properly. Given the strict formatting we have:
    // item { SettingsSectionHeader("...") }
    // item { SettingsTextRow(...) }
    // item { Spacer(...) }
    // We can do a line by line replacement
    
    let lines = advancedBlock.split('\n');
    let inItem = false;
    let bracketCount = 0;
    
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        if (line.match(/^\s*item\s*\{\s*$/)) {
            lines[i] = ''; // remove 'item {' open
            inItem = true;
            bracketCount = 1;
            continue;
        } else if (line.match(/^\s*item\s*\{\s*.*\}\s*$/)) {
            // single line item
            lines[i] = line.replace(/^\s*item\s*\{\s*(.*?)\s*\}\s*$/, '                $1');
        } else if (inItem) {
            // Check for closing brace
            // Count braces on this line
            let open = (line.match(/\{/g) || []).length;
            let close = (line.match(/\}/g) || []).length;
            bracketCount += open - close;
            
            if (bracketCount <= 0) {
                // This is the closing brace
                lines[i] = ''; // remove closing brace
                inItem = false;
                bracketCount = 0;
            }
        }
    }
    
    advancedBlock = lines.join('\n');
    
    // Also we need to fix the closing braces for Box and Column that we changed.
    // Box had a closing brace at the very end. The inner Column had one.
    // The LazyColumn had one.
    // Let's replace the last 3 closing braces with just 1 or 2.
    advancedBlock = advancedBlock.replace(/            }\n        }\n    }\n}\n$/, '    }\n}\n');
    
    // Now insert it back
    code = code.substring(0, startIdx) + advancedBlock + code.substring(endIdx);
    
    // Last step, insert 'item { AdvancedConfigurationContent() }' after Routing settings in ToolsScreen
    code = code.replace(
        /item \{ ToolListItem\(Icons\.Default\.Route, "Routing settings", "Configure rule-based routing settings"\) \{ activeSubScreen = "Routing" \} \}/,
        'item { ToolListItem(Icons.Default.Route, "Routing settings", "Configure rule-based routing settings") { activeSubScreen = "Routing" } }\n                    item { AdvancedConfigurationContent() }'
    );
    
    fs.writeFileSync(file, code, 'utf-8');
    console.log("Transformation completed successfully!");
} else {
    console.log("Could not find blocks");
}
