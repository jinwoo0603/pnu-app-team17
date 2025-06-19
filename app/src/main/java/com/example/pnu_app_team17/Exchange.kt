package com.example.pnu_app_team17

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.net.ssl.*

object Exchange {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private const val API_KEY = "i11e3uF8vWmnCA2bbmkVgSum4DhlbHp8"

    private fun trustAllHosts() {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        try {
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchExchangeData(searchDate: LocalDate): JSONArray {
        val dateStr = searchDate.format(dateFormatter)
        val urlStr =
            "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey=$API_KEY&searchdate=$dateStr&data=AP01"

        trustAllHosts()
        val response = withContext(Dispatchers.IO) {
            URL(urlStr).readText()
        }

        return JSONArray(response)
    }

    suspend fun get(context: Context, searchDate: LocalDate, curUnit: CurUnit, foreignAmount: Double): Int {
        val jsonArray = fetchExchangeData(searchDate)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("cur_unit") == curUnit.unit) {
                val rateStr = obj.getString("kftc_bkpr").replace(",", "")
                val rate = rateStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid rate")

                val isPerHundred = curUnit.unit.contains("(100)")
                val baseAmount = if (isPerHundred) foreignAmount / 100 else foreignAmount

                return (baseAmount * rate).toInt()
            }
        }

        throw IllegalArgumentException("Currency ${curUnit.unit} not found in exchange data")
    }
}