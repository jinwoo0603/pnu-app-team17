package com.example.pnu_app_team17

import android.content.Context
import com.example.pnu_app_team17.Sobi
import com.example.pnu_app_team17.filterByMonth
import com.example.pnu_app_team17.toCsvString
import com.example.pnu_app_team17.categoryProportions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object GPT {
    private const val OPENAI_API_KEY = "your-api-key" // TODO: API 키 넣을것
    private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"

    // 소비 조언 메서드
    suspend fun advise(context: Context): String = withContext(Dispatchers.IO) {
        val items = Sobi.get(context)
        val thisMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val monthlyItems = items.filterByMonth(thisMonth)

        if (monthlyItems.isEmpty()) {
            return@withContext "이번 달 소비 기록이 없습니다."
        }

        val csv = monthlyItems.toCsvString()
        val proportions = monthlyItems.categoryProportions()
        val topCategory = proportions.maxByOrNull { it.value }?.key ?: "N/A"

        val prompt = """
            다음은 한 사용자의 이번 달 소비 내역입니다 (CSV 형식):

            $csv

            카테고리별 소비 비율은 다음과 같습니다:
            ${proportions.entries.joinToString("\n") { (cat, percent) -> "- $cat: %.1f%%".format(percent) }}

            소비 내역을 바탕으로 3~4줄 정도의 간단한 조언을 주세요. 예산 조절, 과소비 경고, 긍정적인 평가 등이 포함되면 좋습니다.
        """.trimIndent()

        try {
            val response = requestToChatGPT(prompt)
            extractReply(response)
        } catch (e: Exception) {
            "GPT 분석 실패: ${e.message}"
        }
    }

    // TODO: 카테고리 종류가 확정되면 영수증 OCR 처리 메서드 만들것

    // GPT API 호출 담당 메서드
    private fun requestToChatGPT(prompt: String): String {
        val client = OkHttpClient()

        val body = JSONObject()
        body.put("model", "gpt-3.5-turbo")
        body.put("messages", listOf(JSONObject(mapOf("role" to "user", "content" to prompt))))
        body.put("temperature", 0.7)

        val requestBody = body.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(OPENAI_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("응답 실패: ${response.code}")
            return response.body?.string() ?: throw IOException("빈 응답")
        }
    }

    // API가 반환한 JSON을 파싱해 결과를 반환하는 메서드
    private fun extractReply(json: String): String {
        val obj = JSONObject(json)
        val content = obj
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
        return content
    }
}
