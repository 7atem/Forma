package com.formacoach.app

import com.formacoach.model.Food
import com.formacoach.model.Nutrients
import com.formacoach.model.NutritionSource
import com.formacoach.model.NutritionUnit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class OnlineFoodResult(
    val code: String,
    val name: String,
    val brand: String,
    val nutrientsPer100g: Nutrients,
) {
    fun toFood(): Food = Food(
        name = if (brand.isNotBlank() && !name.contains(brand, ignoreCase = true)) "$name ($brand)".take(120) else name.take(120),
        referenceAmount = 100.0,
        unit = NutritionUnit.GRAMS,
        nutrients = nutrientsPer100g,
        source = if (code.isNotBlank()) "Open Food Facts (ODbL) · #$code" else "Open Food Facts (ODbL)",
        sourceKind = NutritionSource.LABEL,
    )
}

/**
 * Free, public, keyless Open Food Facts lookup client (ODbL).
 * Uses Android's built-in HttpURLConnection and org.json with strict timeouts and bounded reads.
 */
object OpenFoodFactsClient {
    private const val USER_AGENT = "Forma/0.6 (Android; +https://world.openfoodfacts.org)"
    private const val TIMEOUT_MS = 8_000
    private const val MAX_BYTES = 512 * 1024

    suspend fun search(query: String, preferArabic: Boolean): Result<List<OnlineFoodResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@runCatching emptyList()
            val digitsOnly = trimmed.filter { it.isDigit() }
            // Treat 8..14 digit numeric queries as an EAN/UPC barcode lookup first.
            if (digitsOnly.length in 8..14 && digitsOnly.length == trimmed.length) {
                val byCode = fetchBarcode(digitsOnly, preferArabic)
                if (byCode != null) return@runCatching listOf(byCode)
            }
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=15&fields=code,product_name,product_name_ar,brands,nutriments"
            val body = httpGet(url)
            val root = JSONObject(body)
            val products = root.optJSONArray("products") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until products.length()) {
                    val item = products.optJSONObject(i) ?: continue
                    parseProduct(item, preferArabic)?.let(::add)
                }
            }
        }
    }

    private fun fetchBarcode(barcode: String, preferArabic: Boolean): OnlineFoodResult? {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=code,product_name,product_name_ar,brands,nutriments"
        val body = httpGet(url)
        val root = JSONObject(body)
        if (root.optInt("status", 0) != 1) return null
        val product = root.optJSONObject("product") ?: return null
        return parseProduct(product, preferArabic)
    }

    private fun parseProduct(obj: JSONObject, preferArabic: Boolean): OnlineFoodResult? {
        val code = obj.optString("code", "").trim()
        val arName = obj.optString("product_name_ar", "").trim()
        val baseName = obj.optString("product_name", "").trim()
        val name = if (preferArabic && arName.isNotBlank()) arName else baseName.ifBlank { arName }
        if (name.isBlank()) return null
        val brand = obj.optString("brands", "").split(",").firstOrNull()?.trim().orEmpty()
        val nutriments = obj.optJSONObject("nutriments") ?: return null
        val kcal = nutriments.optDouble("energy-kcal_100g", Double.NaN).takeIf { it.isFinite() && it >= 0.0 }
            ?: nutriments.optDouble("energy-kj_100g", Double.NaN).takeIf { it.isFinite() && it >= 0.0 }?.div(4.184)
            ?: return null
        val protein = nutriments.optDouble("proteins_100g", 0.0).takeIf { it.isFinite() && it in 0.0..100.0 } ?: 0.0
        val carbs = nutriments.optDouble("carbohydrates_100g", 0.0).takeIf { it.isFinite() && it in 0.0..100.0 } ?: 0.0
        val fat = nutriments.optDouble("fat_100g", 0.0).takeIf { it.isFinite() && it in 0.0..100.0 } ?: 0.0
        if (kcal > 2000.0) return null
        val nutrients = runCatching { Nutrients(kcal, protein, carbs, fat) }.getOrNull() ?: return null
        return OnlineFoodResult(code = code, name = name, brand = brand, nutrientsPer100g = nutrients)
    }

    private fun httpGet(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = conn.responseCode
            check(status in 200..299) { "HTTP $status" }
            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val sb = StringBuilder()
            val buf = CharArray(4096)
            var total = 0
            while (true) {
                val read = reader.read(buf)
                if (read < 0) break
                total += read
                check(total <= MAX_BYTES) { "Response too large" }
                sb.append(buf, 0, read)
            }
            return sb.toString()
        } finally {
            conn.disconnect()
        }
    }
}
