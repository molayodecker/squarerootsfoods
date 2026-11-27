package com.squarerootsfoods.kiosk.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MachineApi(private val prefs: MachinePrefs) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun absolute(path: String): String {
        if (path.isBlank() || path.startsWith("http")) return path
        return prefs.serverBase.trimEnd('/') + "/" + path.trimStart('/')
    }

    suspend fun getGoods(): List<Product> = withContext(Dispatchers.IO) {
        val url = "${prefs.serverBase}/api/app/v1/goods/getGoodsList?machine_no=${prefs.machineNo}"
        val body = get(url)
        val data = body.optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                add(
                    Product(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        priceMinor = item.getLong("price_minor"),
                        lane = item.getInt("lane"),
                        stock = item.getInt("stock"),
                        currency = item.optString("currency", prefs.currency),
                        imageUrl = absolute(item.optString("image_url")),
                        category = item.optString("category"),
                    ),
                )
            }
        }
    }

    suspend fun initializePay(
        reference: String,
        email: String,
        amountMinor: Long,
        currency: String,
        lines: List<CartLine>,
    ): PaySession = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("reference", reference)
            .put("email", email)
            .put("amount_minor", amountMinor)
            .put("currency", currency)
            .put("machine_no", prefs.machineNo)
            .put("items", itemArray(lines))
        val body = post("/api/app/payments/paystack/initialize", payload)
        PaySession(
            reference = body.getString("reference"),
            authorizationUrl = body.getString("authorization_url"),
            amountMinor = body.getLong("amount_minor"),
            currency = body.getString("currency"),
            demo = body.optBoolean("demo"),
            email = email,
        )
    }

    suspend fun verifyPay(reference: String): PayVerification = withContext(Dispatchers.IO) {
        val body = get("${prefs.serverBase}/api/app/payments/paystack/verify?reference=$reference")
        PayVerification(
            status = body.optString("status", "pending"),
            demo = body.optBoolean("demo"),
            message = body.optString("message"),
        )
    }

    suspend fun cashEnabled(): Boolean = withContext(Dispatchers.IO) {
        health().optBoolean("cash", false)
    }

    suspend fun climateFlags(): ClimateFlags = withContext(Dispatchers.IO) {
        val body = health()
        ClimateFlags(
            temperature = body.optBoolean("temperature", false),
            humidity = body.optBoolean("humidity", false),
        )
    }

    suspend fun reportClimate(temperatureC: Float?, humidityPercent: Float?) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("machine_no", prefs.machineNo)
        if (temperatureC != null) payload.put("temperature_c", temperatureC.toDouble())
        if (humidityPercent != null) payload.put("humidity_percent", humidityPercent.toDouble())
        post("/api/app/v1/machine/climate", payload)
    }

    private fun health(): JSONObject = get("${prefs.serverBase}/health")

    suspend fun takeCash(
        reference: String,
        amountMinor: Long,
        currency: String,
        lines: List<CartLine>,
    ): PaySession = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("reference", reference)
            .put("amount_minor", amountMinor)
            .put("currency", currency)
            .put("machine_no", prefs.machineNo)
            .put("items", itemArray(lines))
        val body = post("/api/app/payments/cash", payload)
        PaySession(
            reference = body.getString("reference"),
            authorizationUrl = "",
            amountMinor = body.getLong("amount_minor"),
            currency = body.getString("currency"),
            demo = false,
            email = "cash@machine",
            cash = true,
        )
    }

    suspend fun refund(reference: String, amountMinor: Long, reason: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("reference", reference)
            .put("amount_minor", amountMinor)
            .put("reason", reason)
        post("/api/app/payments/paystack/refund", payload)
    }

    suspend fun reportDeliver(reference: String, lane: Int, qty: Int, ok: Boolean, detail: String) =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("machine_no", prefs.machineNo)
                .put("reference", reference)
                .put("lane", lane)
                .put("qty", qty)
                .put("ok", ok)
                .put("detail", detail)
            post("/api/app/v1/order/updateOrderDeliver", payload)
        }

    suspend fun latestRelease(): AppRelease? = withContext(Dispatchers.IO) {
        val body = get(
            "${prefs.serverBase}/api/app/v2/appLastedVersion?machine_no=${prefs.machineNo}",
        )
        if (!body.optBoolean("update")) return@withContext null
        AppRelease(
            versionName = body.optString("version"),
            versionCode = body.optInt("version_code"),
            url = body.optString("url"),
            sha256 = body.optString("sha256"),
            requiredVersion = body.optString("required_version"),
            notes = body.optString("notes"),
        )
    }

    suspend fun submitInstall(payload: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        post("/api/app/v1/machine/commission", payload)
    }

    suspend fun reportUpgrade(
        number: String,
        beforeVersion: String,
        afterVersion: String,
        state: String,
        progress: Int,
        error: String,
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("machine_no", prefs.machineNo)
            .put("number", number)
            .put("before_version", beforeVersion)
            .put("after_version", afterVersion)
            .put("state", state)
            .put("progress", progress)
            .put("error", error)
        post("/api/app/v1/machine/appUpgradeResult", payload)
    }

    private fun itemArray(lines: List<CartLine>): JSONArray {
        val items = JSONArray()
        lines.forEach { line ->
            items.put(
                JSONObject()
                    .put("id", line.product.id)
                    .put("name", line.product.name)
                    .put("lane", line.product.lane)
                    .put("qty", line.qty)
                    .put("price_minor", line.product.priceMinor),
            )
        }
        return items
    }

    private fun get(url: String): JSONObject {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(errorMessage(text, response.code))
            }
            return JSONObject(text)
        }
    }

    private fun post(path: String, payload: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(prefs.serverBase + path)
            .post(payload.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(errorMessage(text, response.code))
            }
            return JSONObject(text)
        }
    }

    private fun errorMessage(text: String, code: Int): String {
        return try {
            val json = JSONObject(text)
            json.optString("message").ifBlank { "Server error $code" }
        } catch (_: Exception) {
            if (text.isBlank()) "Server error $code" else text
        }
    }
}
