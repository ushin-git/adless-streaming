package app.adless.streaming.data

import app.adless.streaming.domain.MusicSource
import app.adless.streaming.domain.SourceSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SourceRegistry(sources: List<MusicSource>) {
    private val sourcesById = sources.associateBy(MusicSource::id)
    val sources: List<MusicSource> = sources.toList()

    suspend fun search(query: String, enabledIds: Set<String>): List<SourceSearch> = coroutineScope {
        sources.filter { it.id in enabledIds }.map { source ->
            async {
                try {
                    SourceSearch(source.id, source.search(query))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    SourceSearch(source.id, error = failure.message ?: "Source unavailable. Try again.")
                }
            }
        }.awaitAll()
    }

    fun source(id: String): MusicSource? = sourcesById[id]
}

