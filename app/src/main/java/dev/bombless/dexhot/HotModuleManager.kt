package dev.bombless.dexhot

import android.content.Context
import dalvik.system.DexClassLoader
import dev.bombless.dexhot.contract.HotScreen
import java.io.File

class HotModuleManager(context: Context) {
    private val root = File(context.filesDir, "hot-modules").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("hot-modules", Context.MODE_PRIVATE)
    @Volatile private var active: HotScreen? = null
    @Volatile private var activeLoader: ClassLoader? = null

    @Synchronized fun install(version: String, dex: ByteArray, entryClass: String): HotScreen {
        require(version.matches(Regex("[A-Za-z0-9._-]{1,64}"))) { "Invalid version" }
        require(dex.isNotEmpty() && dex.size <= MAX_DEX_BYTES) { "Invalid DEX size" }
        require(entryClass.startsWith("dev.bombless.")) { "Entry class must use dev.bombless.*" }
        val dir = File(root, version).apply { mkdirs() }
        val target = File(dir, "classes.dex")
        val temp = File(dir, "classes.dex.tmp")
        temp.writeBytes(dex)
        check(temp.renameTo(target)) { "Unable to atomically store DEX" }
        val screen = instantiate(target, entryClass)
        prefs.edit().putString("entry:$version", entryClass).putString("previous", prefs.getString("current", null)).putString("current", version).apply()
        active = screen
        return screen
    }

    @Synchronized fun activate(version: String): HotScreen {
        val dir = File(root, version)
        val dex = File(dir, "classes.dex")
        require(dex.isFile) { "Version not found: $version" }
        val entry = prefs.getString("entry:$version", null) ?: error("Entry class missing")
        val screen = instantiate(dex, entry)
        prefs.edit().putString("previous", prefs.getString("current", null)).putString("current", version).apply()
        active = screen
        return screen
    }

    private fun instantiate(dex: File, entry: String): HotScreen {
        val optimized = File(root, "optimized").apply { mkdirs() }
        val loader = DexClassLoader(dex.absolutePath, optimized.absolutePath, null, HotScreen::class.java.classLoader)
        val type = loader.loadClass(entry)
        val instance = type.getDeclaredConstructor().newInstance()
        require(instance is HotScreen) { "Module must implement HotScreen from the host contract" }
        activeLoader = loader // Keep the class loader alive while its composable is in use.
        return instance
    }

    fun current(): HotScreen? = active
    fun currentVersion(): String? = prefs.getString("current", null)
    fun versions(): List<String> = root.listFiles()?.filter { File(it, "classes.dex").isFile }?.map { it.name }?.sortedDescending() ?: emptyList()
    fun rollback(): HotScreen {
        val previous = prefs.getString("previous", null) ?: error("No previous version")
        return activate(previous)
    }

    companion object { const val MAX_DEX_BYTES = 32 * 1024 * 1024 }
}
