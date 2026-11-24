package com.pbweather.weather.data.remote

import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DomainError
import java.io.IOException
import java.io.InterruptedIOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
fun JsonObject.number(name: String): Double? = text(name)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > -900 }
fun JsonObject.integer(name: String): Int? = number(name)?.takeIf { it % 1.0 == 0.0 }?.toInt()

data class ApiPage(val items: List<JsonObject>, val total: Int)
interface QuotaRegistry {
    suspend fun limited(family: String): Boolean
    suspend fun mark(family: String)
}
class PersistentQuotaRegistry(private val prefs: PreferencesRepository, private val clock: Clock) : QuotaRegistry {
    private fun today() = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))).toString()
    override suspend fun limited(family: String): Boolean = prefs.preferences.first().quotaDates[family] == today()
    override suspend fun mark(family: String) = prefs.update { it.copy(quotaDates = it.quotaDates + (family to today())) }
}

@Singleton
class ApiClient @Inject constructor(private val api: PublicDataApi, private val json: Json, private val quota: QuotaRegistry) {
    suspend fun fetch(path: String, params: Map<String, String>, family: String, maxItems: Int = 10000): List<JsonObject> =
        withContext(Dispatchers.IO) {
            if (quota.limited(family)) throw DataException(DomainError.QUOTA)
            val result = mutableListOf<JsonObject>()
            var page = 1
            do {
                val parsed = request(path, params + ("pageNo" to page.toString()), family)
                result += parsed.items
                if (result.size >= parsed.total || result.size >= maxItems || parsed.items.isEmpty()) break
                page++
                if (page > 10) throw DataException(DomainError.INVALID_RESPONSE)
            } while (true)
            if (result.isEmpty()) throw DataException(DomainError.NO_DATA)
            result.take(maxItems)
        }
    private suspend fun request(path: String, params: Map<String, String>, family: String): ApiPage {
        var attempt = 0
        while (true) {
            try {
                val response = api.get(path, params)
                if (response.code() == 429) throw DataException(DomainError.QUOTA, response.code())
                if (response.code() in listOf(401, 403)) {
                    val body = response.errorBody()?.use { it.string().take(8192) }.orEmpty()
                    val code = Regex("<(?:returnReasonCode|resultCode)>([0-9]{1,3})</").find(body)?.groupValues?.get(1)
                    throw DataException(DomainError.KEY_CONFIGURATION, response.code(), code)
                }
                if (!response.isSuccessful) throw DataException(DomainError.UNKNOWN)
                val content = response.body()?.use { it.string() } ?: throw DataException(DomainError.INVALID_RESPONSE)
                return parse(content)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: DataException) {
                if (failure.reason == DomainError.QUOTA) quota.mark(family)
                throw failure
            } catch (_: KeyConfigurationException) {
                throw DataException(DomainError.KEY_CONFIGURATION)
            } catch (_: InterruptedIOException) {
                if (attempt >= 2) throw DataException(DomainError.TIMEOUT)
                delay(250L shl attempt)
                attempt++
            } catch (_: IOException) {
                throw DataException(DomainError.OFFLINE)
            } catch (_: Exception) {
                throw DataException(DomainError.INVALID_RESPONSE)
            }
        }
    }
    fun parse(content: String): ApiPage {
        if (content.trimStart().startsWith("<")) {
            val code = Regex("<(?:returnReasonCode|resultCode)>([^<]+)</").find(content)?.groupValues?.get(1)
            throw DataException(errorFor(code ?: "30"))
        }
        val root = json.parseToJsonElement(content) as? JsonObject ?: throw DataException(DomainError.INVALID_RESPONSE)
        val response = root["response"] as? JsonObject ?: throw DataException(DomainError.INVALID_RESPONSE)
        val header = response["header"] as? JsonObject ?: throw DataException(DomainError.INVALID_RESPONSE)
        val code = header.text("resultCode") ?: throw DataException(DomainError.INVALID_RESPONSE)
        if (code.trimStart('0').isNotEmpty()) throw DataException(errorFor(code))
        val body = response["body"] as? JsonObject ?: throw DataException(DomainError.NO_DATA)
        val container = body["items"]
        val item = if (container is JsonObject) container["item"] else container
        val list = when (item) {
            is JsonArray -> item.mapNotNull { it as? JsonObject }
            is JsonObject -> listOf(item)
            else -> emptyList()
        }
        return ApiPage(list, body.integer("totalCount") ?: list.size)
    }
    private fun errorFor(code: String): DomainError = when (code.trimStart('0')) {
        "3" -> DomainError.NO_DATA
        "22" -> DomainError.QUOTA
        "20", "21", "30", "31", "32" -> DomainError.KEY_CONFIGURATION
        else -> DomainError.INVALID_RESPONSE
    }
}
