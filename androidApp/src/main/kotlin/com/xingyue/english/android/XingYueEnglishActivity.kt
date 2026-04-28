package com.xingyue.english.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.xingyue.english.android.data.PlatformLinkResolver
import com.xingyue.english.android.data.XingYueRepository
import com.xingyue.english.android.ui.XingYueApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XingYueEnglishActivity : ComponentActivity() {
    private companion object {
        const val TAG = "XingYueImport"
    }

    private lateinit var repository: XingYueRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = XingYueRepository(this)
        lifecycleScope.launch {
            repository.initialize()
            handleIncomingIntent(intent)
        }
        setContent {
            XingYueApp(
                repository = repository,
                onImportUri = { uri -> importUri(uri) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch { handleIncomingIntent(intent) }
    }

    private fun importUri(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Importing shared file uriScheme=${uri.scheme} uriHost=${uri.host.orEmpty()}")
            runCatching { repository.importUri(uri) }
                .onSuccess { Log.i(TAG, "Shared file import finished status=${it.status} title=${it.title}") }
                .onFailure { error -> Log.e(TAG, "Shared file import failed uriScheme=${uri.scheme}", error) }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            else -> null
        }
        val text = listOfNotNull(
            intent?.getStringExtra(Intent.EXTRA_TEXT),
            intent?.getStringExtra(Intent.EXTRA_SUBJECT),
            intent?.clipData?.let { clip ->
                (0 until clip.itemCount).joinToString("\n") { index ->
                    val item = clip.getItemAt(index)
                    listOfNotNull(item.text?.toString(), item.uri?.toString()).joinToString("\n")
                }
            }
        ).joinToString("\n")
        val sharedUrl = PlatformLinkResolver.extractFirstHttpUrl(text)
        Log.i(
            TAG,
            "Incoming intent action=${intent?.action.orEmpty()} uriScheme=${uri?.scheme.orEmpty()} " +
                "uriHost=${uri?.host.orEmpty()} hasSharedUrl=${sharedUrl != null}"
        )
        when {
            uri != null && uri.scheme in setOf("http", "https") -> {
                importDirectUrl(uri.toString())
            }
            uri != null -> importUri(uri)
            sharedUrl != null -> {
                importDirectUrl(sharedUrl)
            }
        }
    }

    private fun importDirectUrl(rawUrl: String) {
        val url = PlatformLinkResolver.extractFirstHttpUrl(rawUrl) ?: rawUrl.trim()
        if (url.isBlank()) {
            lifecycleScope.launch {
                repository.reportImportIssue("链接导入失败：没有识别到可导入的 URL。")
            }
            return
        }
        val parsed = runCatching { Uri.parse(url) }.getOrNull()
        lifecycleScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Importing shared URL scheme=${parsed?.scheme.orEmpty()} host=${parsed?.host.orEmpty()}")
            runCatching { repository.importDirectUrl(url) }
                .onSuccess { content ->
                    Log.i(TAG, "Shared URL import finished status=${content.status} title=${content.title}")
                }
                .onFailure { error ->
                    Log.e(TAG, "Shared URL import failed host=${parsed?.host.orEmpty()}", error)
                }
        }
    }
}
