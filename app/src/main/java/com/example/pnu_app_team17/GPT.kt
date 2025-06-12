package com.example.pnu_app_team17

import android.content.Context
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import android.graphics.Bitmap
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream

object GPT {
    private const val OPENAI_API_KEY = "키"
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
        //val topCategory = proportions.maxByOrNull { it.value }?.key ?: "N/A"

        val prompt = """
            다음은 한 사용자의 이번 달 소비 내역입니다 (CSV 형식):

            $csv

            카테고리별 소비 비율은 다음과 같습니다:
            ${proportions.entries.joinToString("\n") { (cat, percent) -> "- $cat: %.1f%%".format(percent) }}

            소비 내역을 바탕으로 불필요한 문장은 최소화하여 2~3줄 정도의 간단한 조언을 주세요. 예산 조절, 과소비 경고, 긍정적인 평가 등이 포함되면 좋습니다.
        """.trimIndent()

        try {
            val response = requestToChatGPT(prompt)
            extractReply(response)
        } catch (e: Exception) {
            "GPT 분석 실패: ${e.message}"
        }
    }

    // 영수증 OCR 처리 메서드
    suspend fun ocr(context: Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val base64Image = encodeBitmapToBase64(bitmap)

        val prompt = """
            다음 정보를 바탕으로 JSON 형식으로만 응답해. 불필요한 설명이나 문장은 출력하지 마.
            
            요청내용: 첨부된 영수증 사진을 인식한 뒤, 적힌 소비 내역을 분류해 JSON 배열 형식으로 정리.
            
            출력형식: [{ "date": 날짜(yyyy-MM-dd), "category": (식비, 교통비, 카페, 쇼핑, 기타 중 하나), "amount": 금액 }, ...]
        """.trimIndent()

        return@withContext try {
            val response = requestImageToChatGPT(prompt, base64Image)
            val jsonArray = JSONArray(extractReply(response))

            var successCount = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val date = obj.optString("date")
                val category = obj.optString("category")
                val amount = obj.optInt("amount", -1)

                if (date.isBlank() || category.isBlank() || amount < 0) continue

                try {
                    val parsedDate = LocalDate.parse(date)
                    Sobi.add(context, parsedDate, category, amount)
                    successCount++
                } catch (e: DateTimeParseException) {
                    continue
                }
            }

            "총 ${jsonArray.length()}건 중 ${successCount}건의 소비 내역이 저장되었습니다."

        } catch (e: Exception) {
            "GPT 분석 실패: ${e.message}"
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // GPT API 호출 담당 메서드
    private fun requestToChatGPT(prompt: String): String {
        val client = OkHttpClient()

//        val body = JSONObject()
//        body.put("model", "gpt-3.5-turbo")
//        body.put("messages", listOf(JSONObject(mapOf("role" to "user", "content" to prompt))))
//        body.put("temperature", 0.7)

        val body = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", listOf(JSONObject(mapOf("role" to "user", "content" to prompt))))
            put("temperature", 0.7)
        }

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
    private fun requestImageToChatGPT(prompt: String, base64Image: String): String {
        val client = OkHttpClient()

        // system prompt 추가
        val systemMessage = JSONObject().apply {
            put("role", "system")
            put("content", "You are an assistant that always responds **only in JSON format**. Do not include any explanations or extra text.")
        }

        // user message
        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            })
        }

        // 전체 메시지 배열
        val messages = JSONArray().apply {
            put(systemMessage)
            put(userMessage)
        }

        val body = JSONObject().apply {
            put("model", "gpt-4.1")
            put("messages", messages)
            put("max_tokens", 1000)
        }

        val requestBody = body.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(OPENAI_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("응답 실패: ${response.code}")
            return response.body?.string() ?: throw Exception("빈 응답")
        }
    }
    // API가 반환한 JSON을 파싱해 결과를 반환하는 메서드
    private fun extractReply(json: String): String {
        return JSONObject(json)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}
