import java.io.File

rootProject {
    tasks.register("fixSettingsFull") {
        doLast {
            val file = file("app/src/main/java/com/example/ToolsScreen.kt")
            var code = file.readText()
            
            // 1. Remove DummyItem
            code = code.replace(Regex("DummyItem \\{ ([\\s\\S]*?) \\}")) {
                it.groups[1]?.value ?: ""
            }
            code = code.replace("@Composable\nfun DummyItem(content: @Composable () -> Unit) { content() }\n", "")
            
            // 2. Mux handling to Choice
            code = code.replace(
                "activeTextFieldKey = \"handlingQuicMux\"",
                "activeChoiceFieldKey = \"handlingQuicMux\""
            )
            code = code.replace(
                "activeTextFieldTitle = \"Handling of QUIC in mux tunnel\"",
                "activeChoiceFieldTitle = \"Handling of QUIC in mux tunnel\"\n                            activeChoiceOptions = listOf(\"reject\", \"allow\", \"skip\")"
            )
            code = code.replace(
                "activeTextFieldValue = handlingQuicMux",
                "activeChoiceValue = handlingQuicMux"
            )
            
            // 3. Fragment packets to Choice
            code = code.replace(
                "activeTextFieldKey = \"fragmentPackets\"",
                "activeChoiceFieldKey = \"fragmentPackets\""
            )
            code = code.replace(
                "activeTextFieldTitle = \"Fragment Packets\"",
                "activeChoiceFieldTitle = \"Fragment Packets\"\n                            activeChoiceOptions = listOf(\"tlshello\", \"1-2\", \"1-3\", \"1-5\")"
            )
            code = code.replace(
                "activeTextFieldValue = fragmentPackets",
                "activeChoiceValue = fragmentPackets"
            )
            
            // 4. Update the when block for Choice
            code = code.replace(
                "\"outboundDomainPreResolve\" -> outboundDomainPreResolve = option",
                "\"outboundDomainPreResolve\" -> outboundDomainPreResolve = option\n                                        \"handlingQuicMux\" -> handlingQuicMux = option\n                                        \"fragmentPackets\" -> fragmentPackets = option"
            )
            
            // 5. Remove them from the textfield when block
            code = code.replace("                            \"handlingQuicMux\" -> handlingQuicMux = trimmedVal\n", "")
            code = code.replace("                            \"fragmentPackets\" -> fragmentPackets = trimmedVal\n", "")
            code = code.replace("                            \"vpnBypassLan\" -> vpnBypassLan = trimmedVal\n", "")
            code = code.replace("                            \"vpnInterfaceAddr\" -> vpnInterfaceAddr = trimmedVal\n", "")
            
            // 6. Colors Theme updating
            code = code.replace("Color(0xFF1E88E5)", "MaterialTheme.colorScheme.primary")
            code = code.replace("Color(0xFF263238)", "MaterialTheme.colorScheme.onSurface")
            code = code.replace("Color(0xFF5E6A75)", "MaterialTheme.colorScheme.onSurfaceVariant")
            code = code.replace("Color(0xFF90A4AE)", "MaterialTheme.colorScheme.onSurfaceVariant")
            code = code.replace("Color(0xFF757575)", "MaterialTheme.colorScheme.onSurfaceVariant")
            code = code.replace("Color.Black", "MaterialTheme.colorScheme.onSurface")
            // for white, onPrimary inside OK button, checking checkmark color etc.
            code = code.replace("checkmarkColor = Color.White", "checkmarkColor = MaterialTheme.colorScheme.onPrimary")
            
            // Note: buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            // with Text("OK", color = Color.White), let's replace Color.White with MaterialTheme.colorScheme.onPrimary inside OK text
            code = code.replace("Text(\"OK\", color = Color.White)", "Text(\"OK\", color = MaterialTheme.colorScheme.onPrimary)")
            
            file.writeText(code)
            println("All Transform Success!")
        }
    }
}
