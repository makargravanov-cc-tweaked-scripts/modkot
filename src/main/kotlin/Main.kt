@file:OptIn(ExperimentalTime::class)

package ru.lazyhat

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.time.ExperimentalTime

const val MODPACK_CONFIG_FILENAME = "modpack-config.yaml"
const val MODS_DIRECTORY = "mods"
val modsDirFile = File(MODS_DIRECTORY)
val configFile = File(MODPACK_CONFIG_FILENAME)

fun main() =
    runBlocking {
        try {
            runConsoleLoop()
        } finally {
            client.close()
        }
    }

suspend fun runConsoleLoop() {
    val isModrinthReached = pingModrinth()
	
    while (true) {
        val modConfig =
            configFile.takeIf { it.exists() }?.readBytes()?.decodeToString()?.let {
                Yaml.default
                    .decodeFromString<ModPackConfig>(
                        it,
                    )
            }
        val installedModInfos = getInstalledMods()
        val (uninstalled, unexpected) = modConfig?.let { checkInstallation(it, installedModInfos) } ?: (null to null)
		
        println("Options:")
        println("0. Exit")
        if (isModrinthReached) {
            if (uninstalled?.isNotEmpty() ?: false || unexpected?.isNotEmpty() ?: false) {
                println("1. Update mods use config (overwrite if needed)")
            }
            if (modConfig != null) {
                println("2. Check updates from Modrinth")
            }
            if (modConfig != null) {
                println("3. Migrate to other version (overwrite exiting config)")
            }
            if (modConfig != null) {
                println("4. Migrate to other loader (overwrite exiting config)")
            }
            if (modConfig != null) {
                println("5. Update config from Modrinth (overwrite exiting config)")
            }
        }
        if (unexpected?.isNotEmpty() ?: false) {
            println("6. List unexpected installed mods")
        }
        if (uninstalled?.isNotEmpty() ?: false) {
            println("7. List uninstalled mods")
        }
		
        val option = readln().filter { it.isDigit() }.take(10).toInt()
		
        when (option) {
            0 -> return
            1 if (uninstalled != null || unexpected != null) -> fixInstallation(uninstalled, unexpected)
            2 if modConfig != null -> findNewerVersionsOfMods(modConfig)
            3 if modConfig != null -> {
                print("Enter version of minecraft: ")
                val newVersion = readln()
                if (!newVersion.matches(Regex("1\\.[0-9]{2}\\.[0-9]{1,2}"))) {
                    print("Minecraft version must match pattern #.##.#")
                } else {
                    migrateScript(modConfig, newVersion, modConfig.modLoader)
                }
            }
            4 if modConfig != null -> {
                print("Enter loader name (${ModLoader.entries.map { it.name.lowercase() }.joinToString(",") { it }}): ")
                val newModLoader = readln().uppercase()
                val parsedLoader = ModLoader.entries.find { it.name == newModLoader }
                if (parsedLoader == null) {
                    println("Loader with name doesn't founded")
                } else {
                    migrateScript(modConfig, modConfig.minecraftVersion, parsedLoader)
                }
            }
            5 if modConfig != null -> migrateScript(modConfig, modConfig.minecraftVersion, modConfig.modLoader)
            6 if unexpected != null -> listUnexpectedInstalledMods(unexpected)
            7 if uninstalled != null -> listUninstalledMods(uninstalled)
            else -> {
                println("Unknown option")
                delay(1000)
            }
        }
		
        println()
    }
}

fun checkInstallation(
    modPackConfig: ModPackConfig,
    installedModInfos: Iterable<InstalledModInfo>,
): Pair<Set<ModPackConfig.Version>?, Set<InstalledModInfo>?> {
    val configModNames = modPackConfig.modList.map { it.modName }
    val installedModNames = installedModInfos.map { it.fileName }
	
    val unexpectedInstalledMods = installedModInfos.filter { it.fileName !in configModNames }.toSet().takeIf { it.isNotEmpty() }
    val uninstalledMods =
        modPackConfig.modList
            .filter { it.modName !in installedModNames }
            .toSet()
            .takeIf { it.isNotEmpty() }
	
    return uninstalledMods to unexpectedInstalledMods
}

fun listUninstalledMods(uninstalledMods: Collection<ModPackConfig.Version>) {
    uninstalledMods.forEachIndexed { index, mod ->
        println("$index. Slug: ${mod.slug}, Version: ${mod.versionNumber}, ID: ${mod.versionId}")
    }
}

fun listUnexpectedInstalledMods(unexpectedInstalledMods: Collection<InstalledModInfo>) {
    unexpectedInstalledMods.forEachIndexed { index, mod ->
        println("$index. Mod: ${mod.fullFileName}")
    }
}

suspend fun fixInstallation(
    uninstalledMods: Collection<ModPackConfig.Version>?,
    unexpectedInstalledMods: Collection<InstalledModInfo>?,
) {
    unexpectedInstalledMods?.let {
        removeUnexpectedMods(it)
    }
	
    uninstalledMods?.let {
        val resolvedMods = resolveModsFromConfig(it)
        downloadMods(resolvedMods.filterValuesNotNull())
    }
}

suspend fun findNewerVersionsOfMods(modConfig: ModPackConfig) {
    val resolvedModsFromConfig = resolveModsFromConfig(modConfig.modList)
    val newerVersionsOfMods =
        findNewVersionsOfModrinthMods(
            modConfig.minecraftVersion,
            modConfig.modLoader,
            resolvedModsFromConfig,
        )
	
    if (newerVersionsOfMods.isNotEmpty()) {
        println("Founded new mods versions: ")
        newerVersionsOfMods.entries.forEachIndexed { index, (k, v) ->
            println("$index. ID: ${k.slug}")
            v.forEachIndexed { index, info ->
                print("- ")
                info.printModInfoLn(index)
            }
        }
    } else {
        println("You have latest versions of mods")
    }
}

suspend fun migrateScript(
    modConfig: ModPackConfig,
    proposedMinecraftVersion: String? = null,
    proposedLoader: ModLoader? = null,
) {
    if (proposedMinecraftVersion == null && proposedLoader == null) {
        return
    }
	
    val newMinecraftVersion = proposedMinecraftVersion ?: modConfig.minecraftVersion
    val newModLoader = proposedLoader ?: modConfig.modLoader
	
    val foundedCompatibleMods =
        modConfig.modList
            .map { mod ->
                getModVersionsFromModrinth(
                    mod.slug,
                    newMinecraftVersion,
                    newModLoader,
                ).filter { info ->
                    info.gameVersions?.any { it == newMinecraftVersion } ?: false &&
                        info.loaders?.any { it == newModLoader } ?: false
                }.takeIf { it.isNotEmpty() }
                    ?.maxBy { it.datePublished }
                    .let { mod to it }
            }
	
    val unresolvedCompatibleMods = foundedCompatibleMods.filter { it.second == null }
	
    if (unresolvedCompatibleMods.isNotEmpty()) {
        println("Founded unresolved mods with minecraftVersion: $proposedMinecraftVersion and loader: $proposedLoader")
        unresolvedCompatibleMods.forEachIndexed { index, mod ->
            println("$index. slug: ${mod.first.slug}")
        }
        println("continue?")
        if (readln() != "y") {
            return
        }
    }
	
    val newModList =
        foundedCompatibleMods.map {
            ModPackConfig.Version(
                it.first.slug,
                it.second?.versionNumber,
                it.second?.id,
            )
        }
	
    val newModConfig =
        modConfig.copy(
            minecraftVersion = newMinecraftVersion,
            modLoader = newModLoader,
            modList = newModList,
        )
	
    if (configFile.exists()) {
        configFile.writeText(Yaml.default.encodeToString(ModPackConfig.serializer(), newModConfig))
    }
	
    println("Config successfully updated")
}
