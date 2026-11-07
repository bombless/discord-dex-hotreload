package dev.bombless.dexhot.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bombless.dexhot.contract.HotScreen

/** Sample screen compiled separately and loaded from a DEX file. */
class SampleHotScreen : HotScreen {
    override val version: String = "sample-1"

    @Composable
    override fun Content() {
        val clicks = remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("动态 Compose 页面", style = MaterialTheme.typography.headlineSmall)
            Text("这是从独立 DEX 模块加载的界面。")
            Text("点击次数：${clicks.intValue}")
            Button(onClick = { clicks.intValue++ }) { Text("点击测试") }
        }
    }
}
