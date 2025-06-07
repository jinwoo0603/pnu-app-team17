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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var budgetText: TextView

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

        val goalPrefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
        val goalAmount = goalPrefs.getInt("total_goal", 0)

        val spendPrefs = getSharedPreferences("spending_prefs", MODE_PRIVATE)
        val actualAmount = spendPrefs.getInt("total_spent", 0)

        budgetText.text = "목표 소비액 : %,d원\n실제 소비액 : %,d원".format(goalAmount, actualAmount)

        // 실제 소비 데이터로 파이차트 표시
        lifecycleScope.launch {
            val items = Sobi.get(this@MainActivity)
            showPieChart(items)
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
        
        
        // ### 리셋 버튼 , 삭제 예정
        findViewById<Button>(R.id.buttonReset).setOnClickListener {
            val spendPrefs = getSharedPreferences("spending_prefs", MODE_PRIVATE)
            spendPrefs.edit().putInt("total_spent", 0).apply()

            val goalPrefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
            goalPrefs.edit().putInt("total_goal", 0).apply()

            budgetText.text = "목표 소비액 : 0원\n실제 소비액 : 0원"
            pieChart.clear()
            pieChart.centerText = "소비 비율 없음"
            Toast.makeText(this, "초기화 완료", Toast.LENGTH_SHORT).show()
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

        val data = PieData(dataSet)

        pieChart.apply {
            this.data = data
            description = Description().apply { text = "" }
            isDrawHoleEnabled = true
            setEntryLabelColor(Color.BLACK)
            setUsePercentValues(true)
            centerText = "소비 분석"
            animateY(1000)
            invalidate()
        }
    }
}
