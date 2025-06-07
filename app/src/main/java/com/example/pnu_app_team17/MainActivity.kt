package com.example.pnu_app_team17

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 목표 금액, 실제 소비 금액 SharedPreferences에서 불러오기
        val goalPrefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
        val goalAmount = goalPrefs.getInt("total_goal", 0)

        val spendPrefs = getSharedPreferences("spending_prefs", MODE_PRIVATE)
        val actualAmount = spendPrefs.getInt("total_spent", 0)

        // 텍스트뷰 표시
        val budgetText = findViewById<TextView>(R.id.budgetText)
        budgetText.text = "목표 소비액 : %,d원\n실제 소비액 : %,d원".format(goalAmount, actualAmount)

        // 파이 차트 예시 (하드코딩)
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val entries = listOf(
            PieEntry(40f, "식비"),
            PieEntry(20f, "교통비"),
            PieEntry(15f, "카페"),
            PieEntry(25f, "배달")
        )
        val dataSet = PieDataSet(entries, "소비 분석")
        dataSet.setColors(
            listOf(
                Color.parseColor("#A5D6A7"),
                Color.parseColor("#81C784"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#4CAF50")
            )
        )
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description = Description().apply { text = "" }
        pieChart.centerText = "소비 분석"
        pieChart.animateY(1000)
        pieChart.invalidate()

        // 경고 문구 (고정)
        val warningText = findViewById<TextView>(R.id.warningText)
        warningText.text = "이번달에는\n- 교통\n- 카페\n- 배달\n항목에서 예상보다 많은 지출을 했습니다."

        // 버튼 이벤트
        findViewById<Button>(R.id.buttonHistory).setOnClickListener {
            startActivity(Intent(this, SpendingHistoryActivity::class.java))
        }

        findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            startActivity(Intent(this, ReceiptAddActivity::class.java))
        }

        findViewById<Button>(R.id.buttonGoal).setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }
    }
}
