package dev.gaddal.sifr.feature.tools.data

import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.feature.tools.data.dto.RatesDto
import dev.gaddal.sifr.feature.tools.domain.CurrencyError
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import kotlinx.serialization.SerializationException
import java.time.Instant
import java.time.ZoneOffset

/** Fetches the latest rates. Never throws — maps every failure to a typed [CurrencyError]. */
interface CurrencyApi {
    suspend fun fetchLatest(base: String = "USD"): Result<RatesSnapshot, CurrencyError>
}

class KtorCurrencyApi(
    private val client: HttpClient,
    private val baseUrl: String, // Constants.BASE_URL, e.g. https://open.er-api.com/v6/
) : CurrencyApi {

    override suspend fun fetchLatest(base: String): Result<RatesSnapshot, CurrencyError> {
        return try {
            val response: HttpResponse = client.get("${baseUrl}latest/$base")
            if (!response.status.isSuccess()) {
                return Result.Error(CurrencyError.SERVER_ERROR)
            }
            val dto: RatesDto = response.body()
            if (dto.result != "success" || dto.rates.isEmpty()) {
                return Result.Error(CurrencyError.SERVER_ERROR)
            }
            val asOf = Instant.ofEpochSecond(dto.timeLastUpdateUnix)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            Result.Success(
                RatesSnapshot(
                    base = dto.baseCode ?: base,
                    rates = dto.rates,
                    asOf = asOf,
                ),
            )
        } catch (e: IOException) {
            Result.Error(CurrencyError.NO_NETWORK)
        } catch (e: ContentConvertException) {
            // Ktor wraps deserialization failures (incl. SerializationException) in
            // ContentConvertException, so this — not the bare catch below — is the path
            // a malformed JSON body actually takes.
            Result.Error(CurrencyError.SERIALIZATION)
        } catch (e: SerializationException) {
            Result.Error(CurrencyError.SERIALIZATION)
        } catch (e: Exception) {
            Result.Error(CurrencyError.UNKNOWN)
        }
    }
}
