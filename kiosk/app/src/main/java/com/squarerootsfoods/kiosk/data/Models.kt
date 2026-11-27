package com.squarerootsfoods.kiosk.data

data class ClimateFlags(
    val temperature: Boolean,
    val humidity: Boolean,
)

data class Product(
    val id: String,
    val name: String,
    val priceMinor: Long,
    val lane: Int,
    val stock: Int,
    val currency: String,
    val imageUrl: String = "",
    val category: String = "",
)

data class CartLine(
    val product: Product,
    val qty: Int,
) {
    val totalMinor: Long get() = product.priceMinor * qty
}

data class PaySession(
    val reference: String,
    val authorizationUrl: String,
    val amountMinor: Long,
    val currency: String,
    val demo: Boolean,
    val email: String,
    val cash: Boolean = false,
)

data class PayVerification(
    val status: String,
    val demo: Boolean,
    val message: String,
)

data class AppRelease(
    val versionName: String,
    val versionCode: Int,
    val url: String,
    val sha256: String,
    val requiredVersion: String,
    val notes: String,
)

fun formatMoney(minor: Long, currency: String): String {
    val sign = if (minor < 0) "-" else ""
    val abs = kotlin.math.abs(minor)
    val major = abs / 100
    val cents = abs % 100
    val label = if (currency.equals("GHS", ignoreCase = true)) "GH₵" else currency
    return "$sign$label $major.${cents.toString().padStart(2, '0')}"
}
