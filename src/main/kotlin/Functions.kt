package ru.lazyhat

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.io.RawSink
import kotlinx.io.asSink
import java.io.File
import kotlin.time.ExperimentalTime

val modrinthBaseUrl = "https://api.modrinth.com"
val modrinthV2Url = "$modrinthBaseUrl/v2"

val client =
    HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

suspend fun pingModrinth(): Boolean = client.get(modrinthBaseUrl).status == HttpStatusCode.OK

suspend fun resolveModsFromConfig(configMods: Iterable<ModPackConfig.Version>): Map<ModPackConfig.Version, ModrinthModInfo?> {
    println("Resolving mods from modrinth")
	
    return configMods.associateWith { mod -> mod.versionId?.let { getModVersionFromModrinth(it) } }
}

// suspend fun resolveInstalledMods(installedModsInfo: Iterable<InstalledModInfo>): Map<InstalledModInfo, ModrinthModInfo> =
//    installedModsInfo
//        .mapNotNull { mod ->
//            getModVersionFromModrinth(mod.versionId)?.let { mod to it }
//        }.toMap()

@OptIn(ExperimentalTime::class)
suspend fun findNewVersionsOfModrinthMods(
    minecraftVersion: String,
    loader: ModLoader,
    mods: Map<ModPackConfig.Version, ModrinthModInfo?>,
): Map<ModPackConfig.Version, Set<ModrinthModInfo>> {
    println("Search new versions")
	
    return mods
        .mapNotNull { (key, value) ->
            val projectIdOrSlug = value?.projectId ?: key.slug
            getModVersionsFromModrinth(projectIdOrSlug, minecraftVersion, loader)
                .filter { response ->
                    value?.let { response.datePublished > it.datePublished } ?: true &&
                        response.gameVersions?.any { it == minecraftVersion } ?: false &&
                        response.loaders?.any { it == loader } ?: false
                }.sortedByDescending { it.datePublished }
                .takeIf { it.isNotEmpty() }
                ?.let { key to it.toSet() }
        }.toMap()
}

fun getInstalledMods(): Set<InstalledModInfo> =
    if (!modsDirFile.exists()) {
        emptySet()
    } else {
        modsDirFile
            .list()!!
            .map { InstalledModInfo(it) }
            .toSet()
    }

fun removeUnexpectedMods(unexpectedInstallations: Collection<InstalledModInfo>) {
    println("deleting unexpected mods")

    if (modsDirFile.exists()) {
        modsDirFile.listFiles().forEach {
            if (it.name in unexpectedInstallations.map { it.fullFileName }) {
                it.delete()
				
                println("Mod: ${it.name} deleted")
            }
        }
    }
}

suspend fun downloadMods(currentVersions: Map<ModPackConfig.Version, ModrinthModInfo>) {
    println("downloading")
	
    val modDir = File(MODS_DIRECTORY)
	
    if (!modDir.exists()) {
        modDir.mkdir()
    }
	
    currentVersions.forEach { (k, v) ->
        val fileInfo =
            v.files
                .find {
                    it.primary
                } ?: error("ID ${k.slug}, Version: ${k.versionNumber} primary file not found")
		
        val fileSize = fileInfo.size.toLong()
		
        val file = File(modDir, "${v.id}-${k.slug}-${k.versionNumber}.jar")
		
        val downloadedCount =
            file.outputStream().asSink().downloadFileFrom(fileInfo.url)
		
        check(fileSize == downloadedCount)
		
        println("${k.slug}, ${k.versionNumber} Successfully saved")
    }
}

@OptIn(ExperimentalTime::class)
fun ModrinthModInfo.printModInfoLn(index: Int) {
    println("$index. Name: $name, Version: $versionNumber, Date: $datePublished")
}

val bufferSize: Long = 1024 * 1024

suspend fun RawSink.downloadFileFrom(url: String): Long {
    var count = 0L
    client.prepareGet(url).execute { response ->
        val channel: ByteReadChannel = response.body()
        use {
            while (!channel.exhausted()) {
                val chunk = channel.readRemaining(bufferSize)
                count += chunk.remaining
					
                chunk.transferTo(this)
            }
        }
    }
    return count
}

suspend fun getModVersionsFromModrinth(
    projectId: String,
    minecraftVersion: String? = null,
    loader: ModLoader? = null,
): List<ModrinthModInfo> {
    val loaders = loader?.let { "loaders=[\"${it.name.lowercase()}\"]" }
    val gameVersions = minecraftVersion?.let { "game_versions=[\"$it\"]" }
	
    val queryParameters =
        listOfNotNull(loaders, gameVersions)
            .takeIf { it.isNotEmpty() }
            ?.let { "?${it.joinToString("&")}" } ?: ""
	
    return client
        .get("$modrinthV2Url/project/$projectId/version$queryParameters")
        .body<List<ModrinthModInfo>>()
}

// suspend fun getModVersionFromModrinth(
//    projectIdOrSlug: String,
//    versionIdOrVersionNumber: String,
// ): ModrinthModInfo? =
//    client
//        .get("$modrinthV2Url/project/$projectIdOrSlug/version/$versionIdOrVersionNumber")
//        .let {
//            if (it.status == HttpStatusCode.OK)	it.body<ModrinthModInfo>() else null
//        }

suspend fun getModVersionFromModrinth(versionId: String): ModrinthModInfo? =
    client.get("$modrinthV2Url/version/$versionId").let {
        if (it.status == HttpStatusCode.OK) it.body() else null
    }

fun <K, V> Map<K, V?>.filterValuesNotNull(): Map<K, V> = mapNotNull { (k, v) -> v?.let { k to it } }.toMap()
