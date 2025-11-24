package ru.lazyhat

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ModPackConfig(
    val minecraftVersion: String,
    val modLoader: ModLoader,
    val modList: List<Version>,
) {
    @Serializable
    data class Version(
        val slug: String,
        val versionNumber: String? = null,
        val versionId: String? = null,
    ) {
        @Transient
        val modName: String = "$versionId-$slug-$versionNumber"
    }
}
