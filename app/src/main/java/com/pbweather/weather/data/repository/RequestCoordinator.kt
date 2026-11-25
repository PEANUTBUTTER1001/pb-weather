package com.pbweather.weather.data.repository

import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.SourceMeta
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class RequestKey(val location: String, val kind: DataKind)
data class RequestStatus(val refreshing: Boolean = false, val error: DomainError? = null)
data class LoadedSnapshot(val payload: String, val meta: SourceMeta, val expiresAt: Long? = null)

@Singleton
class RequestCoordinator @Inject constructor(private val cache: CacheStore) {
    private val flights = ConcurrentHashMap<RequestKey, CompletableDeferred<Boolean>>()
    private val mutable = MutableStateFlow<Map<RequestKey, RequestStatus>>(emptyMap())
    val states: StateFlow<Map<RequestKey, RequestStatus>> = mutable.asStateFlow()
    suspend fun refresh(key: String, kind: DataKind, load: suspend () -> LoadedSnapshot): Boolean {
        val request = RequestKey(key, kind)
        val pending = CompletableDeferred<Boolean>()
        val running = flights.putIfAbsent(request, pending)
        if (running != null) return running.await()
        try {
            val existing = cache.read(key, kind)
            if (existing != null && !cache.policy.expired(existing.expiresAt)) return true.also { pending.complete(it) }
            mutable.update { it + (request to RequestStatus(refreshing = true)) }
            try {
                withContext(cache.requestContext()) {
                    val loaded = load()
                    cache.write(key, kind, loaded.payload, loaded.meta, loaded.expiresAt)
                }
                mutable.update { it + (request to RequestStatus()) }
                return true.also { pending.complete(it) }
            } catch (cancelled: CancellationException) {
                pending.completeExceptionally(cancelled)
                throw cancelled
            } catch (failure: Exception) {
                val error = (failure as? DataException)?.reason ?: DomainError.INVALID_RESPONSE
                mutable.update { it + (request to RequestStatus(error = error)) }
                return false.also { pending.complete(it) }
            } finally {
                mutable.update { old ->
                    val status = old[request] ?: RequestStatus()
                    old + (request to status.copy(refreshing = false))
                }
            }
        } finally {
            flights.remove(request, pending)
            if (!pending.isCompleted) pending.complete(false)
        }
    }
}
