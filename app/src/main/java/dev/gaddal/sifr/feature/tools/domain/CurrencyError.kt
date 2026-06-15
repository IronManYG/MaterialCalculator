package dev.gaddal.sifr.feature.tools.domain

import dev.gaddal.sifr.core.domain.util.Error

enum class CurrencyError : Error {
    NO_NETWORK,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN,
}
