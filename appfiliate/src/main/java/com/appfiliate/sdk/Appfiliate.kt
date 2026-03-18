package com.appfiliate.sdk

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

public object Appfiliate {

    private const val TAG = "Appfiliate"
    private const val SDK_VERSION = "1.0.0"
    private const val PREFS_NAME = "appfiliate_prefs"
    private const val KEY_TRACKED = "install_tracked"
    private const val KEY_ATTRIBUTION_ID = "attribution_id"
    private const val KEY_MATCHED = "matched"

    private var appId: String? = null
    private var apiKey: String? = null
    private var apiBase = "https://us-central1-appfiliate-5a18b.cloudfunctions.net/api"
    private var isConfigured = false

    private val executor = Executors.newSingleThreadExecutor()

    // MARK: - Configuration

    /**
     * Configure Appfiliate with your app credentials.
     * Call once in Application.onCreate() or your main Activity.
     *
     * ```kotlin
     * Appfiliate.configure(this, appId = "app_xxx", apiKey = "key_xxx")
     * ```
     */
    @JvmStatic
    fun configure(context: Context, appId: String, apiKey: String) {
        this.appId = appId
        this.apiKey = apiKey
        this.isConfigured = true
    }

    /**
     * Override the API base URL (for testing/development).
     */
    @JvmStatic
    fun setAPIBase(url: String) {
        this.apiBase = url
    }

    // MARK: - Install Attribution

    /**
     * Track app install attribution. Call once on first app launch, after configure().
     * Automatically reads the Google Play Install Referrer for deterministic matching.
     *
     * ```kotlin
     * Appfiliate.trackInstall(this)
     * ```
     */
    @JvmStatic
    fun trackInstall(context: Context, callback: ((AttributionResult) -> Unit)? = null) {
        if (!isConfigured) {
            Log.e(TAG, "Call Appfiliate.configure() before trackInstall()")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_TRACKED, false)) {
            // Already tracked — return cached result
            val cached = AttributionResult(
                matched = prefs.getBoolean(KEY_MATCHED, false),
                attributionId = prefs.getString(KEY_ATTRIBUTION_ID, null),
                confidence = 0.0,
                method = "cached",
                clickId = null
            )
            callback?.invoke(cached)
            return
        }

        // Try to read the Install Referrer (deterministic attribution)
        readInstallReferrer(context) { referrer ->
            val payload = buildPayload(context, referrer)
            sendAttribution(context, payload, callback)
        }
    }

    // MARK: - Purchase Tracking

    /**
     * Track an in-app purchase attributed to the install.
     *
     * ```kotlin
     * Appfiliate.trackPurchase(
     *     context = this,
     *     productId = "premium_monthly",
     *     revenue = 9.99,
     *     currency = "USD",
     *     transactionId = "GPA.1234-5678"
     * )
     * ```
     */
    @JvmStatic
    fun trackPurchase(
        context: Context,
        productId: String,
        revenue: Double,
        currency: String = "USD",
        transactionId: String? = null
    ) {
        if (!isConfigured) {
            Log.e(TAG, "Call Appfiliate.configure() before trackPurchase()")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attributionId = prefs.getString(KEY_ATTRIBUTION_ID, null)

        if (attributionId == null) {
            Log.w(TAG, "No attribution ID found. Install may not have been attributed.")
            return
        }

        val payload = JSONObject().apply {
            put("app_id", appId)
            put("attribution_id", attributionId)
            put("product_id", productId)
            put("revenue", revenue)
            put("currency", currency)
            put("sdk_version", SDK_VERSION)
            if (transactionId != null) put("transaction_id", transactionId)
        }

        executor.execute {
            val result = post("/v1/purchases", payload)
            if (result != null) {
                Log.d(TAG, "Purchase tracked: $productId $revenue $currency")
            } else {
                Log.e(TAG, "Purchase tracking failed")
            }
        }
    }

    // MARK: - Public Helpers

    /**
     * Check if this install was attributed to a creator.
     */
    @JvmStatic
    fun isAttributed(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MATCHED, false)
    }

    /**
     * The attribution ID for this install (null if not attributed).
     */
    @JvmStatic
    fun getAttributionId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ATTRIBUTION_ID, null)
    }

    // MARK: - Private

    private fun readInstallReferrer(context: Context, callback: (String?) -> Unit) {
        try {
            val client = InstallReferrerClient.newBuilder(context).build()
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            val referrer = client.installReferrer.installReferrer
                            Log.d(TAG, "Install referrer: $referrer")
                            client.endConnection()
                            callback(referrer)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to read referrer: ${e.message}")
                            client.endConnection()
                            callback(null)
                        }
                    } else {
                        Log.w(TAG, "Install referrer not available: $responseCode")
                        client.endConnection()
                        callback(null)
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.w(TAG, "Install referrer service disconnected")
                    callback(null)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Install referrer client error: ${e.message}")
            callback(null)
        }
    }

    private fun buildPayload(context: Context, referrer: String?): JSONObject {
        val metrics = context.resources.displayMetrics

        return JSONObject().apply {
            put("app_id", appId)
            put("platform", "android")
            put("device_model", Build.MODEL)
            put("device_manufacturer", Build.MANUFACTURER)
            put("os_version", Build.VERSION.RELEASE)
            put("sdk_int", Build.VERSION.SDK_INT)
            put("screen_width", metrics.widthPixels)
            put("screen_height", metrics.heightPixels)
            put("screen_density", metrics.density)
            put("timezone", TimeZone.getDefault().id)
            put("language", Locale.getDefault().language)
            put("languages", JSONArray(Locale.getDefault().let {
                listOf(it.toLanguageTag())
            }))
            put("hw_concurrency", Runtime.getRuntime().availableProcessors())
            put("sdk_version", SDK_VERSION)

            // Install Referrer (deterministic — the key signal for Android)
            if (referrer != null) {
                put("referrer", referrer)
                // Extract our click ID if present
                val clickId = parseReferrerParam(referrer, "af_click_id")
                if (clickId != null) {
                    put("af_click_id", clickId)
                }
            }
        }
    }

    private fun parseReferrerParam(referrer: String, key: String): String? {
        return referrer.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
    }

    private fun sendAttribution(
        context: Context,
        payload: JSONObject,
        callback: ((AttributionResult) -> Unit)?
    ) {
        executor.execute {
            val json = post("/v1/attribution", payload)
            if (json != null) {
                val result = AttributionResult(
                    matched = json.optBoolean("matched", false),
                    attributionId = json.optString("attribution_id", null),
                    confidence = json.optDouble("confidence", 0.0),
                    method = json.optString("method", "unknown"),
                    clickId = json.optString("click_id", null)
                )

                // Cache result
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_TRACKED, true)
                    .putBoolean(KEY_MATCHED, result.matched)
                    .putString(KEY_ATTRIBUTION_ID, result.attributionId)
                    .apply()

                Log.d(TAG, "Install attribution: matched=${result.matched}, confidence=${result.confidence}, method=${result.method}")
                callback?.invoke(result)
            } else {
                Log.e(TAG, "Attribution request failed")
                callback?.invoke(
                    AttributionResult(false, null, 0.0, "error", null)
                )
            }
        }
    }

    private fun post(endpoint: String, payload: JSONObject): JSONObject? {
        return try {
            val url = URL(apiBase + endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-API-Key", apiKey)
            conn.setRequestProperty("X-App-ID", appId)
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                JSONObject(body)
            } else {
                Log.e(TAG, "API error: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}")
            null
        }
    }

    // MARK: - Data Classes

    data class AttributionResult(
        val matched: Boolean,
        val attributionId: String?,
        val confidence: Double,
        val method: String,
        val clickId: String?
    )
}
