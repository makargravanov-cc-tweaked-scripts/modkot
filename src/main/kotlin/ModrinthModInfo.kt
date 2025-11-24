package ru.lazyhat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
@OptIn(ExperimentalTime::class)
data class ModrinthModInfo(
    val name: String?,
    @SerialName("version_number")
    val versionNumber: String?,
    @SerialName("changelog")
    val changeLog: String?,
    val dependencies: List<ModrinthModDependency>?,
    @SerialName("game_versions")
    val gameVersions: List<String>?,
    @SerialName("version_type")
    val versionType: VersionType?,
    val loaders: List<ModLoader>?,
    val featured: Boolean?,
    val status: String?,
    @SerialName("requested_status")
    val requestedStatus: String?,
    val id: String,
    @SerialName("project_id")
    val projectId: String,
    @SerialName("author_id")
    val authorId: String,
    @SerialName("date_published")
    val datePublished: Instant,
    val downloads: Int,
    @SerialName("changelog_url")
    val changeLogUrl: String?,
    val files: List<ModrinthFile>,
)

@Serializable
data class ModrinthFile(
    val hashes: Map<String, String>,
    val url: String,
    @SerialName("filename")
    val fileName: String,
    val primary: Boolean,
    val size: Int,
    @SerialName("file_type")
    val fileType: String?,
)

@Serializable
data class ModrinthModDependency(
    @SerialName("version_id")
    val versionId: String?,
    @SerialName("project_id")
    val projectId: String?,
    @SerialName("file_name")
    val fileName: String?,
    @SerialName("dependency_type")
    val dependencyType: ModrinthModDependencyType,
)
