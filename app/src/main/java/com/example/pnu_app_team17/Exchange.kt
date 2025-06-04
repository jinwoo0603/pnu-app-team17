package com.example.pnu_app_team17

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//환율 API 처리 object
object Exchange {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private const val PREF_NAME = "exchange_pref"
    private const val EXCHANGE_DATE_KEY = "exchange_date"
    private const val EXCHANGE_VALUES_KEY = "exchange_values"
    private const val API_KEY = "i11e3uF8vWmnCA2bbmkVgSum4DhlbHp8" // 그냥 API 키 깔게요 어차피 유료도 아닌데

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    //API를 내부적으로 호출하는 메소드
    private suspend fun callAPI(context: Context, searchdate: LocalDate) {
        val dateStr = searchdate.format(dateFormatter)
        val urlStr =
            "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey=$API_KEY&searchdate=$dateStr&data=AP01"

        val response = withContext(Dispatchers.IO) {
            URL(urlStr).readText()
        }

        val prefs = getPrefs(context)
        prefs.edit()
            .putString(EXCHANGE_DATE_KEY, dateStr)
            .putString(EXCHANGE_VALUES_KEY, response)
            .apply()
    }

    //
    //Exchange.get(context, LocalDate.now(), CurUnit.USD) 이런식으로 사용
    suspend fun get(context: Context, searchdate: LocalDate, curUnit: CurUnit, foreignAmount: Double): Int {
        val prefs = getPrefs(context)
        val storedDate = prefs.getString(EXCHANGE_DATE_KEY, null)
        val targetDate = searchdate.format(dateFormatter)

        if (storedDate != targetDate) {
            callAPI(context, searchdate)
        }

        val jsonStr = prefs.getString(EXCHANGE_VALUES_KEY, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("cur_unit") == curUnit.unit) {
                val rateStr = obj.getString("kftc_bkpr").replace(",", "")
                val rate = rateStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid rate")

                // 100 단위인지 확인
                val isPerHundred = curUnit.unit.contains("(100)")
                val baseAmount = if (isPerHundred) foreignAmount / 100 else foreignAmount

                val result = (baseAmount * rate).toInt()
                return result
            }
        }

        throw IllegalArgumentException("Currency ${curUnit.unit} not found in exchange data")
    }
}
