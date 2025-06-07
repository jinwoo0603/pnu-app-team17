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

    private var actualAmount = 180_100  // 실제 소비액은 예시로 하드코딩되어 있음 (나중에 동적 연동 가능)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // SharedPreferences에서 목표 금액 불러오기 (없으면 0)
        val prefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
        val goalAmount = prefs.getInt("total_goal", 0)

        // 소비 금액 텍스트 설정
        val budgetText = findViewById<TextView>(R.id.budgetText)
        budgetText.text = "목표 소비액 : %,d원\n실제 소비액 : %,d원".format(goalAmount, actualAmount)

        // 원형 차트 설정 (예시)
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

        // 경고 문구 (임시 고정)
        val warningText = findViewById<TextView>(R.id.warningText)
        warningText.text = "이번달에는\n- 교통\n- 카페\n- 배달\n항목에서 예상보다 많은 지출을 했습니다."

        // 버튼들 이벤트 연결
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
