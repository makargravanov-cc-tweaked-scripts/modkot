package ru.lazyhat

data class InstalledModInfo(
    val fullFileName: String,
) {
    val fileName: String = fullFileName.removeSuffix(".jar")
    // val versionId: String = fileName.substringBefore("-")
}
