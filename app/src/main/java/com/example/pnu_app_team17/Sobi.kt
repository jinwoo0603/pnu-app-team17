package com.example.pnu_app_team17

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.example.pnu_app_team17.Category

object Sobi {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 소비 기록 추가
    fun add(context: Context, date: LocalDate, category: String, amount: Int) {
        val db = FirebaseFirestore.getInstance()
        //로그인 여부 검증 및 현재 아이디 가져오기
        val id = Auth.currentId(context) ?: run {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Category.values().none { it.tag == category }) {
            Toast.makeText(context, "유효하지 않은 카테고리입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val sobi = hashMapOf(
            "id" to id,
            "date" to date.format(dateFormatter),
            "category" to category,
            "amount" to amount
        )

        db.collection("sobi")
            .add(sobi)
            .addOnSuccessListener {
                Toast.makeText(context, "소비 기록 추가됨", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "소비 기록 추가 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // 전체 소비 기록 조회
    // launch로 비동기적으로 함수를 호출해 소비내역을 리스트로 받아오고, SobiExtensions의 확장함수들을 체이닝해 로직 수행
    //ex)
    //lifecycleScope.launch {
    //    val items = Sobi.get(this@MainActivity)
    //
    //    val thisMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    //    val monthlyItems = items.filterByMonth(thisMonth)
    //
    //    val total = monthlyItems.totalAmount()
    //    val sorted = monthlyItems.sortedByDate()
    //    val grouped = monthlyItems.groupByCategory()
    //    val avgByCat = monthlyItems.averageAmountByCategory()
    //    val topCats = monthlyItems.topCategories(3)
    //
    //    Log.d("소비총합", "$total 원")
    //    Log.d("카테고리별 평균", avgByCat.toString())
    //    Log.d("Top 카테고리", topCats.toString())
    //}
    suspend fun get(context: Context): List<SobiItem> {
        val db = FirebaseFirestore.getInstance()
        val id = Auth.currentId(context) ?: return emptyList()

        return try {
            val result = db.collection("sobi")
                .whereEqualTo("id", id)
                .get()
                .await()

            result.mapNotNull { doc ->
                val date = doc.getString("date")
                val category = doc.getString("category")
                val amount = doc.getLong("amount")?.toInt()
                if (date != null && category != null && amount != null) {
                    SobiItem(doc.id, date, category, amount)
                } else null
            }
        } catch (e: Exception) {
            Toast.makeText(context, "소비 기록 조회 실패", Toast.LENGTH_SHORT).show()
            emptyList()
        }
    }

    // 특정 소비 기록 삭제
    fun delete(context: Context, documentId: String) {
        if (!Auth.isLoggedIn(context)) {
            Toast.makeText(context, "로그인 오류", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("sobi")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "삭제 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
    }
}
