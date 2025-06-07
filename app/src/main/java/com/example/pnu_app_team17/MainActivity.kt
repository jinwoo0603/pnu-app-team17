package com.example.pnu_app_team17

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var budgetText: TextView
    private lateinit var warningText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pieChart = findViewById(R.id.pieChart)
        budgetText = findViewById(R.id.budgetText)
        warningText = findViewById(R.id.warningText)

        val goalPrefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
        val goalAmount = goalPrefs.getInt("total_goal", 0)

        val spendPrefs = getSharedPreferences("spending_prefs", MODE_PRIVATE)
        val actualAmount = spendPrefs.getInt("total_spent", 0)

        budgetText.text = "목표 소비액 : %,d원\n실제 소비액 : %,d원".format(goalAmount, actualAmount)

        lifecycleScope.launch {
            val items = Sobi.get(this@MainActivity)
            showPieChart(items)
            showWarningIfOverspent()
        }

        findViewById<Button>(R.id.buttonHistory).setOnClickListener {
            startActivity(Intent(this, SpendingHistoryActivity::class.java))
        }

        findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            startActivity(Intent(this, ReceiptAddActivity::class.java))
        }

        findViewById<Button>(R.id.buttonGoal).setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }

        // ### 초기화 코드 곡 삭제 예정ㅅ
        findViewById<Button>(R.id.buttonReset).setOnClickListener {
            val userId = Auth.currentId(this)
            if (userId == null) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            getSharedPreferences("spending_prefs", MODE_PRIVATE)
                .edit().putInt("total_spent", 0).apply()
            getSharedPreferences("goal_prefs", MODE_PRIVATE)
                .edit().putInt("total_goal", 0).apply()

            val db = FirebaseFirestore.getInstance()
            db.collection("sobi").whereEqualTo("id", userId).get().addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    db.collection("sobi").document(doc.id).delete()
                }
            }
            db.collection("goals").whereEqualTo("id", userId).get().addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    db.collection("goals").document(doc.id).delete()
                }
            }

            budgetText.text = "목표 소비액 : 0원\n실제 소비액 : 0원"
            pieChart.apply {
                clear()
                centerText = "소비 비율 없음"
                invalidate()
            }
            warningText.text = "초기화 완료"
            Toast.makeText(this, "모든 소비 데이터 및 목표가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPieChart(items: List<SobiItem>) {
        val proportions = items.categoryProportions()
        if (proportions.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "소비 비율 없음"
            return
        }

        val entries = proportions.map { (category, percent) ->
            PieEntry(percent.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "카테고리별 소비").apply {
            sliceSpace = 2f
            valueTextSize = 12f
            colors = listOf(
                Color.parseColor("#FFA726"), // 주황
                Color.parseColor("#66BB6A"), // 녹색
                Color.parseColor("#42A5F5"), // 파랑
                Color.parseColor("#EF5350"), // 빨강
                Color.parseColor("#AB47BC")  // 보라
            )
        }

        pieChart.apply {
            data = PieData(dataSet)
            description = Description().apply { text = "" }
            isDrawHoleEnabled = true
            setEntryLabelColor(Color.BLACK)
            setUsePercentValues(true)
            centerText = "소비 분석"
            animateY(1000)
            invalidate()
        }
    }

    private suspend fun showWarningIfOverspent() {
        val userId = Auth.currentId(this) ?: return
        val db = FirebaseFirestore.getInstance()

        val goalSnapshot = db.collection("goals")
            .whereEqualTo("id", userId)
            .get().await()

        val goalMap = goalSnapshot.documents.associate {
            val category = it.getString("category") ?: ""
            val amount = it.getLong("amount")?.toInt() ?: 0
            category to amount
        }

        val sobiList = Sobi.get(this)
        val actualMap = sobiList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val overSpent = actualMap.filter { (category, spent) ->
            val goal = goalMap[category] ?: 0
            spent > goal
        }.keys

        warningText.text = if (overSpent.isEmpty()) {
            "이달의 소비는 모두 목표 이내입니다."
        } else {
            val overText = overSpent.joinToString(", ")
            "이달에는 $overText 항목에서 초과 지출하였습니다."
        }
    }
}
