package br.unb.ru.data.repository

import android.content.Context
import br.unb.ru.data.model.Campus
import br.unb.ru.data.model.WeeklyMenu
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MenuRepository(private val context: Context) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("ru_unb_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val KEY_THEME_CUSTOMIZED = "theme_customized"
        private const val KEY_SELECTED_CAMPUS = "selected_campus"
    }

    fun getSelectedCampus(): Campus {
        val id = prefs.getString(KEY_SELECTED_CAMPUS, Campus.DARCY.id)
        return Campus.fromId(id)
    }

    fun saveSelectedCampus(campus: Campus) {
        prefs.edit().putString(KEY_SELECTED_CAMPUS, campus.id).apply()
    }

    fun saveThemePreference(isDark: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_DARK_MODE, isDark)
            .putBoolean(KEY_THEME_CUSTOMIZED, true)
            .apply()
    }

    fun getSavedThemePreference(): Boolean? {
        val customized = prefs.getBoolean(KEY_THEME_CUSTOMIZED, false)
        return if (customized) {
            prefs.getBoolean(KEY_IS_DARK_MODE, false)
        } else {
            null
        }
    }

    suspend fun getMenu(campus: Campus = getSelectedCampus()): Result<WeeklyMenu> = withContext(Dispatchers.IO) {
        val remoteUrl = "https://raw.githubusercontent.com/Cl0udShifter/ru-unb-cardapio/master/parser/${campus.remoteFileName}"
        try {
            val url = URL(remoteUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val menu = gson.fromJson(jsonString, WeeklyMenu::class.java)
                if (menu != null && menu.dias.isNotEmpty()) {
                    saveToCache(campus, jsonString)
                    return@withContext Result.success(menu)
                }
            }
        } catch (_: Exception) {}

        val cachedMenu = loadFromCache(campus)
        if (cachedMenu != null && cachedMenu.dias.isNotEmpty()) {
            return@withContext Result.success(cachedMenu)
        }

        try {
            context.assets.open(campus.assetFile).use { stream ->
                InputStreamReader(stream).use { reader ->
                    val assetMenu = gson.fromJson(reader, WeeklyMenu::class.java)
                    if (assetMenu != null) {
                        return@withContext Result.success(assetMenu)
                    }
                }
            }
        } catch (_: Exception) {
            try {
                context.assets.open("sample_menu.json").use { stream ->
                    InputStreamReader(stream).use { reader ->
                        val assetMenu = gson.fromJson(reader, WeeklyMenu::class.java)
                        if (assetMenu != null) {
                            return@withContext Result.success(assetMenu)
                        }
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        Result.failure(IllegalStateException("Não foi possível carregar o cardápio."))
    }

    private fun getCacheFileName(campus: Campus) = "cached_menu_${campus.id}.json"

    private fun saveToCache(campus: Campus, jsonString: String) {
        try {
            val file = File(context.filesDir, getCacheFileName(campus))
            file.writeText(jsonString)
        } catch (_: Exception) {}
    }

    private fun loadFromCache(campus: Campus): WeeklyMenu? {
        return try {
            val file = File(context.filesDir, getCacheFileName(campus))
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, WeeklyMenu::class.java)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
