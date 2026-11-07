package dev.bombless.dexhot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bombless.dexhot.contract.HotScreen
import dev.bombless.dexhot.contract.HostApi

class MainActivity : ComponentActivity() {
    private lateinit var manager: HotModuleManager
    private lateinit var server: TcpUpdateServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = HotModuleManager(this)
        server = TcpUpdateServer(this, manager)
        server.start()
        setContent {
            var screen by remember { mutableStateOf(manager.current()) }
            var versions by remember { mutableStateOf(manager.versions()) }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("DEX Hot Reload", style = MaterialTheme.typography.headlineMedium)
                        Text("TCP 127.0.0.1:${TcpUpdateServer.PORT} · Debug only")
                        Text("当前版本：${manager.currentVersion() ?: "未加载"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { runCatching { screen = manager.rollback(); versions = manager.versions() } }) { Text("回退上一版本") }
                            Button(onClick = { versions = manager.versions() }) { Text("刷新版本") }
                        }
                        Text("版本历史", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(versions) { version ->
                                Card {
                                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(version)
                                        Button(onClick = { runCatching { screen = manager.activate(version) } }) { Text("激活") }
                                    }
                                }
                            }
                        }
                        Text("动态模块界面", style = MaterialTheme.typography.titleMedium)
                        val loaded = screen
                        if (loaded != null) loaded.Content()
                        else Text("尚未加载模块。先构建 hotmodule 的 DEX，再通过 TCP 上传。")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        server.stop()
        super.onDestroy()
    }
}
