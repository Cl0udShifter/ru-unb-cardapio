package br.unb.ru.data.model

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)
