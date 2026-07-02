import java.io.File

rootProject {
    tasks.register("removeDummyItems") {
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
                        if (lines[j].contains("{")) braceCount++
                        if (lines[j].contains("}")) braceCount--
                        if (braceCount == 0 && lines[j].trim() == "}") {
                            lines[j] = ""
                            closingFound = true
                            break
                        }
                        j++
                    }
                    if(!closingFound) {
                        println("Warning: matching brace not found for Dummy at line $i")
                    }
                }
                i++
            }
            file.writeText(lines.joinToString("\n"))
            println("Done removing DummyItems!")
        }
    }
}
