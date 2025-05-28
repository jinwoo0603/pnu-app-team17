package com.example.pnu_app_team17

import android.os.Bundle
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
import android.widget.Button
import android.widget.Toast
import android.content.Intent
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    private val goalAmount = 500_000
    private val actualAmount = 180_100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 소비 금액 텍스트 설정
        val budgetText = findViewById<TextView>(R.id.budgetText)
        budgetText.text = "목표 소비액 : %,d원\n실제 소비액 : %,d원".format(goalAmount, actualAmount)

        // 원형 차트 설정
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

        // 경고 문구
        val warningText = findViewById<TextView>(R.id.warningText)
        warningText.text = "이번달에는\n- 교통\n- 카페\n- 배달\n항목에서 예상보다 많은 지출을 했습니다."

        // 버튼 클릭 이벤트 (예시로 토스트만 출력)
        findViewById<Button>(R.id.buttonHistory).setOnClickListener {
            Toast.makeText(this, "소비 내역 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            Toast.makeText(this, "소비 추가 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonGoal).setOnClickListener {
            Toast.makeText(this, "목표 설정 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
    }
}
