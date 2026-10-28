package com.garfiec.librechat.core.model.tchat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * `GET /api/tchat/credit`: the signed-in user's TensorGrid credit. [balanceCents] is in the
 * currency's minor unit, which TensorGrid fixes per currency: USD cents (2 digits), IRT toman (0).
 */
@Serializable
data class TchatCredit(
    @SerialName("balance_cents") val balanceCents: Long,
    val currency: String,
) {
    /** The amount with thousands separators and the currency's own decimals, sign first. */
    fun amountText(): String {
        val digits = if (currency == USD) 2 else 0
        var scale = 1L
        repeat(digits) { scale *= 10 }
        val magnitude = abs(balanceCents)
        val whole = (magnitude / scale).toString().reversed().chunked(3).joinToString(",").reversed()
        val fraction = if (digits == 0) "" else "." + (magnitude % scale).toString().padStart(digits, '0')
        return (if (balanceCents < 0) "-" else "") + whole + fraction
    }

    companion object {
        const val USD = "USD"
        const val IRT = "IRT"
    }
}
