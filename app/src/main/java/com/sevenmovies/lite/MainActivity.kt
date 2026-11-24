package com.sevenmovies.lite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF101318)
private val Panel = Color(0xFF1B2028)
private val Accent = Color(0xFFE45A63)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Accent,
                onBackground = Color.White, onSurface = Color.White
            )) {
                val vm: SearchViewModel = viewModel()
                LiteScreen(vm) { url ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    }
}

data class MovieResult(val title: String, val year: String, val type: String, val url: String)

class SearchViewModel : ViewModel() {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<MovieResult>>(emptyList())
    var loading by mutableStateOf(false)
    var message by mutableStateOf("Search the 7Movies catalog")
    private var job: Job? = null
    private val repository = SevenMoviesRepository()

    fun search(value: String) {
        query = value
        job?.cancel()
        if (value.trim().length < 2) {
            results = emptyList()
            message = "Type at least 2 characters"
            loading = false
            return
        }
        job = viewModelScope.launch {
            delay(350)
            loading = true
            message = ""
            try {
                val found = repository.search(value.trim())
                results = found
                message = if (found.isEmpty()) {
                    "No results found. The site's search route may have changed."
                } else ""
            } catch (_: Exception) {
                results = emptyList()
                message = "Search couldn't connect. Try again or open 7Movies."
            } finally { loading = false }
        }
    }
}

/**
 * Search adapter.
 *
 * IMPORTANT: 7Movies does not publish a documented public search API.
 * This adapter uses a conventional website search URL as a best-effort
 * integration point. Verify/update SEARCH_URL and HTML selectors against
 * the live site's permitted search interface before production release.
 * It does not extract or proxy video streams.
 */
class SevenMoviesRepository {
    companion object {
        private const val SEARCH_URL = "https://7movies.ws/search?query="
    }
    private val client = okhttp3.OkHttpClient.Builder()
        .followRedirects(true).build()

    suspend fun search(term: String): List<MovieResult> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = SEARCH_URL + java.net.URLEncoder.encode(term, "UTF-8")
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android) 7MoviesLite/0.1")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string().orEmpty()
                val doc = org.jsoup.Jsoup.parse(html, "https://7movies.ws")
                val seen = linkedSetOf<String>()
                doc.select("a[href]").mapNotNull { a ->
                    val href = a.absUrl("href")
                    val path = Uri.parse(href).path.orEmpty()
                    if (!href.startsWith("https://7movies.ws/") ||
                        !path.matches(Regex("/(movie|tv)/[A-Za-z0-9_-]+")) ||
                        !seen.add(href)) return@mapNotNull null
                    val title = a.select("h2,h3,[class*=title]").firstOrNull()?.text()
                        ?.takeIf { it.isNotBlank() } ?: a.text().trim()
                    if (title.isBlank()) return@mapNotNull null
                    val meta = a.text()
                    val year = Regex("(19|20)\\d{2}").find(meta)?.value.orEmpty()
                    val type = if (path.startsWith("/tv/")) "TV" else "Movie"
                    MovieResult(title, year, type, href)
                }.take(60)
            }
        }
}

@Composable
fun LiteScreen(vm: SearchViewModel, openPage: (String) -> Unit) {
    Scaffold(containerColor = Ink) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("7MOVIES", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black, color = Color.White)
                Text(" LITE", color = Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().height(54.dp)
                    .padding(0.dp).then(Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color.LightGray)
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = vm.query, onValueChange = vm::search,
                            singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (vm.query.isEmpty()) Text("Search movies or TV shows…", color = Color.Gray)
                                inner()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            if (vm.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
            }
            if (vm.message.isNotBlank()) {
                Text(vm.message, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(vm.results) { item ->
                    Surface(
                        color = Panel, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { openPage(item.url) }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, tint = Accent, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(listOf(item.year, item.type).filter { it.isNotBlank() }.joinToString(" · "),
                                    color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("OPEN", color = Accent, style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { openPage("https://7movies.ws/") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Open 7Movies website", color = Color.Gray)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
