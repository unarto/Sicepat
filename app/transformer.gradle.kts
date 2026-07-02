import java.io.File

tasks.register("rewriteSettings") {
    doLast {
        val file = file("src/main/java/com/example/ToolsScreen.kt")
        var code = file.readText()
        
        // Remove from navigation routing
        code = code.replace(
            "\"Advanced configuration\" -> AdvancedConfigurationScreen(onBack = { activeSubScreen = null })",
            ""
        )
        
        // Remove from Tools config list and insert the settings directly
        code = code.replace(
            "item { ToolListItem(Icons.Default.Build, \"Advanced configuration\", \"Provide diverse configuration options\") { activeSubScreen = \"Advanced configuration\" } }",
            "item { AdvancedConfigurationContent() }"
        )
        
        val startIdx = code.indexOf("@Composable\nfun AdvancedConfigurationScreen(onBack: () -> Unit) {")
        val endIdx = code.indexOf("@Composable\nfun SettingsSectionHeader(title: String) {")
        
        if (startIdx != -1 && endIdx != -1) {
            var block = code.substring(startIdx, endIdx)
            
            block = block.replace(
                "fun AdvancedConfigurationScreen(onBack: () -> Unit) {",
                "fun AdvancedConfigurationContent() {"
            )
            
            val boxStartStrRegex = Regex("Box\\([\\s\\S]*?\\) \\{")
            block = boxStartStrRegex.replaceFirst(block, "Column(modifier = Modifier.fillMaxWidth()) {")
            
            val innerColStartStrRegex = Regex("Column\\(modifier = Modifier\\.fillMaxSize\\(\\)\\) \\{[\\s\\S]*?Text\\(\\s*text = \"Settings\",[\\s\\S]*?color = Color\\(0xFF263238\\)\\s*\\)\\s*\\}")
            block = innerColStartStrRegex.replaceFirst(block, "")
            
            block = block.replace("LazyColumn(modifier = Modifier.fillMaxSize()) {", "Column(modifier = Modifier.fillMaxWidth()) {")
            
            // replace `item {` recursively/safely
            // Since we know `item {` can span multiple lines, let's just cheat and define a dummy `ItemWrapper`
            block = block.replace("item {", "DummyItem {")
            
            // Fix brackets at end
            val bracketsRegex = Regex("\\}\\s*\\}\\s*\\}\\s*\\}\\s*$")
            block = bracketsRegex.replace(block, "}\n}\n")
            
            code = code.substring(0, startIdx) + block + code.substring(endIdx)
            
            // Insert DummyItem below AdvancedConfigurationContent
            code += "\n@Composable\nfun DummyItem(content: @Composable () -> Unit) { content() }\n"
            
            file.writeText(code)
            println("Transform Success!")
        } else {
            println("Failed to find boundaries.")
        }
    }
}
