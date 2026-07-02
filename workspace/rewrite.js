const fs = require('fs');

const file = 'app/src/main/java/com/example/DashboardScreen.kt';
let code = fs.readFileSync(file, 'utf-8');

// To safely replace, let's first fix imports
code = code.replace(
    'import androidx.compose.foundation.layout.*',
    'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.lazy.staggeredgrid.*\nimport androidx.compose.animation.core.animateDpAsState'
);

// We need to change `visibleCards` from Set to List
code = code.replace(
    'var visibleCards by remember { mutableStateOf(DashboardCardType.values().toSet()) }',
    'var visibleCards by remember { mutableStateOf(DashboardCardType.values().toList()) }'
);

// We need to change visibility toggle in AlertDialog
// Instead of + and -, we do plus/minus for List. Lists support + and - but - only removes first occurrence. Since Set elements are unique, it's fine.
// Let's replace visibleCards.contains -> visibleCards.contains
code = code.replace(
    /visibleCards = visibleCards - cardType/g,
    'visibleCards = visibleCards.filter { it != cardType }'
);
code = code.replace(
    /visibleCards = visibleCards \+ cardType/g,
    'visibleCards = (visibleCards + cardType).distinct()'
);

// Now the DashboardCard component: Add onMoveUp and onMoveDown (or Left/Right)
code = code.replace(
    'onClose: () -> Unit,',
    'onClose: () -> Unit,\n    onMoveLeft: (() -> Unit)? = null,\n    onMoveRight: (() -> Unit)? = null,'
);

// In DashboardCard layout, next to close button, we can add left and right arrows
const replaceCardBadge = `
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
                        .background(Color(0xFF81D4FA)) // Light sky/cyan blue circle matching screenshot
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFF0D47A1), // Dark navy blue cross matching screenshot
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
`;

// Find `if (showCloseButton) {` in DashboardCard
code = code.replace(/if \(showCloseButton\) \{[\s\S]*?Icon\([\s\S]*?\}\n        \}/m, replaceCardBadge);


code = code.replace('fun DashboardCard(', '@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n@Composable\nfun DashboardCard(')


// Now the big replacement: the entire scrollable area.
// We start at Box(modifier = Modifier.fillMaxSize()) { Column ... verticalScroll ...
// and we replace it with a LazyVerticalStaggeredGrid
code = code.replace(
    'Column(\n            modifier = Modifier\n                .fillMaxSize()\n                .verticalScroll(rememberScrollState())\n                .padding(bottom = 80.dp) // Space for FAB\n        )',
    'LazyVerticalStaggeredGrid(\n            columns = StaggeredGridCells.Fixed(2),\n            modifier = Modifier.fillMaxSize(),\n            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),\n            horizontalArrangement = Arrangement.spacedBy(12.dp),\n            verticalItemSpacing = 12.dp\n        )'
);

// Notice `padding(horizontal = 16.dp, vertical = 12.dp)` on Header... wait, let's just make it items.
fs.writeFileSync(file, code, 'utf-8');
console.log("Script executed!");
