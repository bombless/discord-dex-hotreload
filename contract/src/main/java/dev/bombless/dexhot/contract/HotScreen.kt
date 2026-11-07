package dev.bombless.dexhot.contract

import androidx.compose.runtime.Composable

/** Implement this interface in a hot-loaded DEX module. */
interface HotScreen {
    val version: String

    /** Called once after construction; keep the host API reference if needed. */
    fun onHostAttached(host: HostApi) {}

    @Composable fun Content()
}

/** Narrow, stable API exposed by the host to dynamically loaded modules. */
interface HostApi {
    fun log(message: String)
    fun currentVersion(): String?
}
