import java.io.File

rootProject {
    tasks.register("fixSettings") {
        doLast {
            val file = file("app/src/main/java/com/example/ToolsScreen.kt")
            var code = file.readText()
            
            // Fix DummyItem {}
            code = code.replace(Regex("DummyItem \\{ (.*?) \\}"), "\$1")
            
            // Fix dialog for Mux handling and fragment packets
            
            // Change handlingQuicMux from text to choice
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
            
            // Change fragmentPackets from text to choice
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
            
            // Inside the dialog switch
            code = code.replace(
                "\"outboundDomainPreResolve\" -> outboundDomainPreResolve = option",
                "\"outboundDomainPreResolve\" -> outboundDomainPreResolve = option\n                                        \"handlingQuicMux\" -> handlingQuicMux = option\n                                        \"fragmentPackets\" -> fragmentPackets = option"
            )
            
            // Fix Settings color theme and background
            // Background is implicitly light grey due to dynamic coloring in modern android?
            // "warna background, tesk, dsb sesuaikan dengan tema"
            // Let's replace the hardcoded colors in SettingsCheckboxRow, SettingsSectionHeader and SettingsTextRow
            
            // Replace hardcoded color in SettingsSectionHeader
            code = code.replace("color = Color(0xFF1E88E5)", "color = MaterialTheme.colorScheme.primary")
            // Replace text color in SettingsCheckboxRow and TextRow
            code = code.replace("color = Color(0xFF263238)", "color = MaterialTheme.colorScheme.onSurface")
            // Replace subtitle color in SettingsCheckboxRow and TextRow
            code = code.replace("color = Color(0xFF5E6A75)", "color = MaterialTheme.colorScheme.onSurfaceVariant")
            // Checkbox checkedColor
            // already replaced 1E88E5 -> primary. Wait, let's just make it explicit 
            
            code = code.replace("checkedColor = MaterialTheme.colorScheme.primary,", "checkedColor = MaterialTheme.colorScheme.primary,")
            code = code.replace("uncheckedColor = Color(0xFF90A4AE),", "uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,")
            code = code.replace("checkmarkColor = Color.White", "checkmarkColor = MaterialTheme.colorScheme.onPrimary")
            
            
            
            file.writeText(code)
            println("Transform Success!")
        }
    }
}
