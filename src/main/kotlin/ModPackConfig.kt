package ru.lazyhat

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ModPackConfig(
    val minecraftVersion: String,
    val modLoader: ModLoader,
    val modList: Map<String, String?>,
) {
    @Transient
    val modVersions = modList.map { (key, value) -> Version(key, value ?: "null") }

    @Serializable
    data class Version(
        val slug: String,
        val versionNumber: String,
    ) {
        val modName: String = "$slug-$versionNumber"
    }
}
