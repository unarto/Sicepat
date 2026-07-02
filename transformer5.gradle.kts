import java.io.File

rootProject {
    tasks.register("removeDummyItemsRobust") {
        doLast {
            val file = file("app/src/main/java/com/example/ToolsScreen.kt")
            val lines = file.readLines().toMutableList()
            
            var i = 0
            while (i < lines.size) {
                if (lines[i].trim() == "DummyItem {") {
                    lines[i] = ""
                    // find matching closing brace
                    var braceCount = 1
                    var j = i + 1
                    var closingFound = false
                    while (j < lines.size) {
                        val openCount = lines[j].count { it == '{' }
                        val closeCount = lines[j].count { it == '}' }
                        braceCount += openCount
                        braceCount -= closeCount
                        
                        // We check if this line closes our DummyItem
                        // If braceCount drops to 0 on this line, then we just remove the first extra `}` 
                        // Actually, if the format is strictly `                }` on its own line:
                        if (braceCount == 0 && lines[j].trim() == "}") {
                            lines[j] = ""
                            closingFound = true
                            break
                        } else if (braceCount == 0) {
                             // replace the last '}'
                             lines[j] = lines[j].replaceAfterLast("}", "").dropLast(1)
                             closingFound = true
                             break
                        }
                        j++
                    }
                    if(!closingFound) {
                        println("Warning: matching brace not found for Dummy at line " + i)
                    }
                }
                i++
            }
            file.writeText(lines.joinToString("\n"))
            println("Done removing DummyItems!")
        }
    }
}
