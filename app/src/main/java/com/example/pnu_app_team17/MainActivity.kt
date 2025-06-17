package com.example.pnu_app_team17

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var budgetText: TextView
    private lateinit var warningText: TextView
    private lateinit var predictionText: TextView

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
        predictionText = findViewById(R.id.predictionText)

        lifecycleScope.launch {
            val userId = Auth.currentId(this@MainActivity)
            if (userId == null) {
                Toast.makeText(this@MainActivity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val db = FirebaseFirestore.getInstance()

            // 🔹 목표 금액 계산
            val goalSnapshot = db.collection("goals")
                .whereEqualTo("id", userId)
                .get()
                .await()
            val totalGoal = goalSnapshot.documents.sumOf {
                it.getLong("amount")?.toInt() ?: 0
            }

            // 🔹 소비 금액 계산
            val sobiSnapshot = db.collection("sobi")
                .whereEqualTo("id", userId)
                .get()
                .await()
            val totalSpent = sobiSnapshot.documents.sumOf {
                it.getLong("amount")?.toInt() ?: 0
            }

            budgetText.text = "목표 소비액 : %,d원\n실제 소비액 : %,d원".format(totalGoal, totalSpent)

            val items = Sobi.get(this@MainActivity)
            showPieChart(items)
            showWarningIfOverspent()
            showSpendingPrediction(items)
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

        findViewById<Button>(R.id.buttonReset).setOnClickListener {
            val userId = Auth.currentId(this)
            if (userId == null) {
                Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
            predictionText.text = ""
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
                Color.parseColor("#FFA726"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#42A5F5"),
                Color.parseColor("#EF5350"),
                Color.parseColor("#AB47BC")
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

    private fun showSpendingPrediction(items: List<SobiItem>) {
        try {
            val model = FileUtil.loadMappedFile(this, "spending_predictor_model.tflite")
            val interpreter = Interpreter(model)
            val predictor = SpendingPredictor(interpreter)
            val prediction = predictor.predictNextSobi(items)

            predictionText.text = "다음 소비 예측: ${prediction.date} | ${prediction.category.tag} | %,d원".format(prediction.amount)
        } catch (e: Exception) {
            predictionText.text = "소비 예측 불가"
            Log.e("PredictionError", "예측 실패: ${e.message}")
        }
    }
}
