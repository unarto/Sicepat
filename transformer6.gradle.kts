import java.io.File

rootProject {
    tasks.register("updateProxiesStyle") {
        doLast {
            val file = file("app/src/main/java/com/example/ProxiesScreen.kt")
            var code = file.readText()
            
            // Add Grid imports if missing
            if (!code.contains("import androidx.compose.foundation.lazy.grid.*")) {
                code = code.replace("import androidx.compose.foundation.lazy.items\n", "import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.grid.*\n")
            }
            
            // Instead of LazyColumn directly, check selectedStyle
            val lazyColumnRegex = Regex("""// List[\s\S]*?LazyColumn\([\s\S]*?\}\n            \}""")
            
            val newCollection = """
            if (selectedStyle == "Tab") {
                val gridColumns = when (selectedSize) {
                    "Standard" -> 2
                    "Shrink" -> 3
                    "Min" -> 3 // or 4
                    else -> 2
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedProxies.size) { index ->
                        val proxy = sortedProxies[index]
                        ProxyCard(
                            proxy = proxy,
                            layout = selectedLayout,
                            size = selectedSize,
                            isGrid = true,
                            onClick = {
                                val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                if (originalIndex != -1) {
                                    for (i in proxiesList.indices) {
                                        proxiesList[i] = proxiesList[i].copy(isSelected = (i == originalIndex))
                                    }
                                }
                            },
                            onEdit = { newName, newType ->
                                val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                if (originalIndex != -1) {
                                    proxiesList[originalIndex] = proxiesList[originalIndex].copy(name = newName, type = newType)
                                }
                            },
                            onDelete = {
                                val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                if (originalIndex != -1) {
                                    proxiesList.removeAt(originalIndex)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp)
                ) {
                    items(sortedProxies.size) { index ->
                        val proxy = sortedProxies[index]
                        ProxyCard(
                            proxy = proxy,
                            layout = selectedLayout,
                            size = selectedSize,
                            isGrid = false,
                            onClick = {
                                val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                if (originalIndex != -1) {
                                    for (i in proxiesList.indices) {
                                        proxiesList[i] = proxiesList[i].copy(isSelected = (i == originalIndex))
                                    }
                                }
                            },
                            onEdit = { newName, newType ->
                                val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                if (originalIndex != -1) {
                                    proxiesList[originalIndex] = proxiesList[originalIndex].copy(name = newName, type = newType)
                                }
                            },
                            onDelete = {
                                val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                if (originalIndex != -1) {
                                    proxiesList.removeAt(originalIndex)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            """.trimIndent()
            
            code = code.replace(lazyColumnRegex, newCollection)
            
            // Adjust ProxyCard signature
            code = code.replace("fun ProxyCard(\n    proxy: ProxyItem,\n    layout: String,\n    size: String,\n    onClick: () -> Unit,", "fun ProxyCard(\n    proxy: ProxyItem,\n    layout: String,\n    size: String,\n    isGrid: Boolean = false,\n    onClick: () -> Unit,")
            
            file.writeText(code)
        }
    }
}
