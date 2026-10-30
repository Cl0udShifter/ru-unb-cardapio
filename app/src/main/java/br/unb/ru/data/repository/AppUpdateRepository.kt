package br.unb.ru.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import br.unb.ru.BuildConfig
import br.unb.ru.data.model.AppUpdateInfo
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class AppUpdateRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("ru_update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/Cl0udShifter/ru-unb-cardapio/releases/latest"
        private const val PREF_LAST_CHECK = "last_update_check_millis"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    suspend fun checkForUpdate(force: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(PREF_LAST_CHECK, 0L)

        if (!force && (now - lastCheck) < CHECK_INTERVAL_MS) {
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "RU-UnB-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val bodyString = response.body?.string() ?: return@withContext null
            val json = JsonParser.parseString(bodyString).asJsonObject

            val tagName = json.get("tag_name")?.asString ?: ""
            val releaseNotes = json.get("body")?.asString ?: ""
            
            // Find APK asset
            var apkDownloadUrl = ""
            val assets = json.getAsJsonArray("assets")
            if (assets != null) {
                for (assetElement in assets) {
                    val assetObj = assetElement.asJsonObject
                    val name = assetObj.get("name")?.asString ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                        break
                    }
                }
            }

            // Fallback download URL if not listed directly in assets
            if (apkDownloadUrl.isBlank() && tagName.isNotBlank()) {
                apkDownloadUrl = "https://github.com/Cl0udShifter/ru-unb-cardapio/releases/download/$tagName/RU-UnB.apk"
            }

            prefs.edit().putLong(PREF_LAST_CHECK, now).apply()

            val currentVersion = BuildConfig.VERSION_NAME
            val isAvailable = isNewerVersion(currentVersion, tagName)

            AppUpdateInfo(
                latestVersion = tagName,
                currentVersion = currentVersion,
                releaseNotes = releaseNotes,
                downloadUrl = apkDownloadUrl,
                isUpdateAvailable = isAvailable
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false
        val currSegments = current.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val latestSegments = latest.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currSegments.size, latestSegments.size)
        for (i in 0 until maxLen) {
            val currVal = currSegments.getOrElse(i) { 0 }
            val latestVal = latestSegments.getOrElse(i) { 0 }
            if (latestVal > currVal) return true
            if (latestVal < currVal) return false
        }
        return false
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "RU-UnB-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()

            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()

            val apkFile = File(updatesDir, "RU-UnB-update.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }
            apkFile
        } catch (e: Exception) {
            null
        }
    }

    fun canInstallApk(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchPackageInstaller(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}
