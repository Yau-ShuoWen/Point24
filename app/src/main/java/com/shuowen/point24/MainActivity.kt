package com.shuowen.point24

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shuowen.point24.game.Point24
import com.shuowen.point24.ui.theme.Point24Theme

class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContent {
            Point24Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Point24App()
                }
            }
        }
    }
}

@Composable
fun Point24App()
{
    val count by remember { mutableStateOf(4) }
    var numbers by remember { mutableStateOf(List(4) { "" }) }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasCalculated by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎮 24 点计算器",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Text("数字个数: ")
//            DropdownMenuWithCount(count) { count = it; numbers = List(it) { "" } }
//        }

        Spacer(modifier = Modifier.height(16.dp))

        val chunkedNumbers = numbers.chunked(2)

        chunkedNumbers.forEachIndexed { rowIndex, rowNumbers ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowNumbers.forEachIndexed { colIndex, value ->
                    val globalIndex = rowIndex * 2 + colIndex

                    OutlinedTextField(
                        value = value,
                        onValueChange = {

                            if (it.length <= 2 && it.all { c -> c.isDigit() })
                            {
                                numbers = numbers.toMutableList().apply {
                                    this[globalIndex] = it
                                }
                            }
                        },
//                        label = { Text("数字${globalIndex + 1}") },  // 显示 1, 2, 3...
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowNumbers.size == 1)
                {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(
                onClick = {
                    numbers = List(count) { "" }
                    results = emptyList()
                    hasCalculated = false
                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("🗑️ 清空")
            }

            OutlinedButton(onClick = {
                keyboardController?.hide()

                isLoading = true
                val inputNumbers =
                    numbers.filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
                hasCalculated = true

                results = if (inputNumbers.size == count)
                {
                    val allSolutions = Point24.calc24(*inputNumbers.toIntArray())
                    if (allSolutions.isEmpty())
                    {
                        listOf("❌ 无解")
                    }
                    else
                    {
                        listOf("✅ 有解（共 ${allSolutions.size} 种）")
                    }
                }
                else
                {
                    listOf("⚠️ 请输入 $count 个有效数字")
                }
                isLoading = false
            },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && numbers.all { it.isNotBlank() }) {
                Text(if (isLoading) "..." else "💡 提示")
            }

            Button(onClick = {
                keyboardController?.hide()

                isLoading = true
                val inputNumbers =
                    numbers.filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
                hasCalculated = true

                results = if (inputNumbers.size == count)
                {
                    Point24.calc24(*inputNumbers.toIntArray())
                }
                else
                {
                    listOf("⚠️ 请输入 $count 个有效数字")
                }
                isLoading = false
            },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && numbers.all { it.isNotBlank() }) {
                Text(if (isLoading) "计算中..." else "🔍 答案")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(24.dp))


        if (hasCalculated)
        {
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = if (results.isEmpty() || results[0].startsWith("⚠️") || results[0] == "❌ 无解")
                    {
                        MaterialTheme.colorScheme.errorContainer
                    }
                    else
                    {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when
                        {
                            results.size == 1 && results[0].startsWith("⚠️") -> results[0]
                            results.isEmpty() -> "❌ 这个组合无解"
                            results[0] == "❌ 无解" -> "❌ 无解"
                            results[0].startsWith("✅ 有解") -> results[0]
                            else -> "✅ 找到 ${results.size} 种解法："
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (results.isEmpty() || results[0].startsWith("⚠️") || results[0] == "❌ 无解")
                        {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                        else
                        {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (results.isNotEmpty() && !results[0].startsWith("⚠️") && results[0] != "❌ 无解" && !results[0].startsWith(
                            "✅ 有解"
                        )
                    )
                    {
                        Spacer(modifier = Modifier.height(8.dp))
                        results.forEach { expr ->
                            Text(
                                text = "$expr = 24",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuWithCount(
    currentCount: Int, onCountChanged: (Int) -> Unit
)
{
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(3, 4, 5, 6)  // 支持3-6个数字

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$currentCount 个")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text("$option 个数字") }, onClick = {
                    onCountChanged(option)
                    expanded = false
                })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Point24AppPreview()
{
    Point24Theme {
        Point24App()
    }
}