package com.anilibrix.plus.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.anilibrix.plus.BuildConfig
import com.anilibrix.plus.core.datastore.SettingsDataStore
import com.anilibrix.plus.domain.model.ChangelogRelease
import com.anilibrix.plus.domain.model.NetworkResult
import com.anilibrix.plus.domain.repository.GitHubRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обновление приложения из релизов GitHub.
 *
 * Приложение распространяется не через магазин, поэтому обновляться человеку
 * приходилось вручную: зайти на GitHub, найти релиз, скачать APK. Всё нужное
 * для этого в коде уже было — `browser_download_url` разбирался из ответа, а
 * [com.anilibrix.plus.ui.components.UpdateSnackbarEffect] был написан, — но
 * ссылка выбрасывалась при маппинге, а снекбар не композился нигде.
 */
@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubRepository: GitHubRepository,
    private val settings: SettingsDataStore,
) {

    /**
     * Проверяет наличие новой версии.
     *
     * Не чаще раза в сутки: чаще незачем, а на каждом холодном старте это
     * лишний сетевой запрос ради почти всегда одинакового ответа.
     */
    suspend fun checkForUpdate(force: Boolean = false): AppUpdate? {
        if (!force) {
            val lastCheck = settings.lastUpdateCheckAt.first()
            if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return null
        }
        settings.setLastUpdateCheckAt(System.currentTimeMillis())

        val result = gitHubRepository.getReleases().first { it !is NetworkResult.Loading }
        val releases = (result as? NetworkResult.Success)?.data.orEmpty()

        // Предрелизы не предлагаем: человек, ставящий приложение из GitHub,
        // не обязан быть бета-тестером.
        val latest = releases.firstOrNull { !it.isPrerelease } ?: return null
        if (!latest.isNewerThanCurrent()) return null

        return AppUpdate(
            version = latest.tagName.removePrefix("v"),
            notes = latest.body,
            apkUrl = latest.apkUrl,
            apkSizeBytes = latest.apkSizeBytes,
            htmlUrl = latest.htmlUrl,
        )
    }

    /**
     * Скачивает APK системным менеджером загрузок.
     *
     * Именно системным, а не своим: он переживает закрытие приложения,
     * показывает прогресс в шторке и умеет докачивать после обрыва — писать
     * это заново ради одного файла в сутки не окупается.
     */
    fun download(update: AppUpdate): Long? {
        val url = update.apkUrl ?: return null
        val fileName = "anilibrix-plus-${update.version}.apk"

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Anilibrix Plus ${update.version}")
            .setDescription("Скачивание обновления")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, UPDATES_DIR, fileName)
            .setMimeType(APK_MIME)

        val manager = context.getSystemService(DownloadManager::class.java) ?: return null
        return runCatching { manager.enqueue(request) }.getOrNull()
    }

    /** Отдаёт скачанный APK системному установщику. */
    fun install(file: File): Boolean {
        if (!file.exists()) return false

        // content:// вместо file://: прямые файловые URI запрещены с API 24,
        // и установщик просто откажется открывать такой Intent.
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** Открывает страницу релиза — запасной путь, если APK к релизу не приложен. */
    fun openReleasePage(update: AppUpdate): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.htmlUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /**
     * Сравнение версий по компонентам.
     *
     * Строковое сравнение здесь врёт: «1.10.0» лексикографически меньше
     * «1.9.0», и обновление до десятой минорной версии не предлагалось бы.
     */
    private fun ChangelogRelease.isNewerThanCurrent(): Boolean {
        val remote = tagName.removePrefix("v").toVersionParts()
        val current = BuildConfig.VERSION_NAME.removePrefix("v").toVersionParts()
        if (remote.isEmpty()) return false

        for (i in 0 until maxOf(remote.size, current.size)) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun String.toVersionParts(): List<Int> =
        split('.', '-').mapNotNull { it.filter(Char::isDigit).toIntOrNull() }

    companion object {
        const val UPDATES_DIR = "updates"
        private const val APK_MIME = "application/vnd.android.package-archive"
        private val CHECK_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
    }
}

data class AppUpdate(
    val version: String,
    val notes: String?,
    val apkUrl: String?,
    val apkSizeBytes: Long,
    val htmlUrl: String,
) {
    /** Можно ли обновиться, не уходя в браузер. */
    val canInstallInApp: Boolean get() = !apkUrl.isNullOrBlank()
}
