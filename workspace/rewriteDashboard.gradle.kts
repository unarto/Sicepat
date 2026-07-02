import java.io.File

rootProject {
    tasks.register("rewriteDashboard") {
        doLast {
            val file = file("app/src/main/java/com/example/DashboardScreen.kt")
            var code = file.readText()
            
            code = code.replace(
                "import androidx.compose.foundation.layout.*",
                "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.lazy.staggeredgrid.*\nimport androidx.compose.animation.core.animateDpAsState"
            )
            
            code = code.replace(
                "var visibleCards by remember { mutableStateOf(DashboardCardType.values().toSet()) }",
                "var visibleCards by remember { mutableStateOf(DashboardCardType.values().toList()) }"
            )
            
            code = code.replace(
                "visibleCards = visibleCards - cardType",
                "visibleCards = visibleCards.filter { it != cardType }"
            )
            
            code = code.replace(
                "visibleCards = visibleCards + cardType",
                "visibleCards = (visibleCards + cardType).distinct()"
            )
            
            code = code.replace(
                "onClose: () -> Unit,",
                "onClose: () -> Unit,\n    onMoveLeft: (() -> Unit)? = null,\n    onMoveRight: (() -> Unit)? = null,"
            )
            
            val badgeReplacement = """
        if (showCloseButton) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onMoveLeft != null) {
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFE0E0E0)).clickable { onMoveLeft() }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (onMoveRight != null) {
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFE0E0E0)).clickable { onMoveRight() }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Move Right", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF81D4FA))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFF0D47A1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
            """.trimIndent()
            
            // replace old close badge logic with new
            val badgeRegex = Regex("if \\(showCloseButton\\) \\{[\\s\\S]*?Icon\\([\\s\\S]*?\\}\\n        \\}")
            code = badgeRegex.replaceFirst(code, badgeReplacement)
            
            // Now we replace the whole Scrollable Column -> Header + Cards Grid into LazyVerticalStaggeredGrid
            
            // Oh actually, rewriting the ENTIRE Layout with regex might explode.
            // Let's write the Dashboard Content dynamically.
            
            file.writeText(code)
            println("Done preliminary replacement!")
        }
    }
}
