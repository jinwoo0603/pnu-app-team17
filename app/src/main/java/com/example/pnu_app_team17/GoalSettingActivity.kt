package com.example.pnu_app_team17

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GoalSettingActivity : AppCompatActivity() {

    // Firebase 연동 예정: 카테고리와 목표 금액 리스트
    private val categories = listOf("식비", "교통비", "배달", "커피")
    private val goalAmounts = mutableMapOf<String, Int>() // 카테고리별 목표 금액 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_setting)

        val container = findViewById<LinearLayout>(R.id.goalContainer)
        findViewById<TextView>(R.id.titleText).text = "목표 설정"

        categories.forEach { category ->
            val row = layoutInflater.inflate(R.layout.item_goal_input, container, false)
            val categoryText = row.findViewById<TextView>(R.id.categoryText)
            val amountInput = row.findViewById<EditText>(R.id.amountInput)

            categoryText.text = category
            amountInput.setText("0")

            amountInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val value = s.toString().replace(",", "").toIntOrNull() ?: 0
                    goalAmounts[category] = value
                }
            })

            container.addView(row)
        }
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            // 저장 버튼 클릭 시 처리할 로직
            // 예: Firebase 업로드, 토스트 출력 등
            Toast.makeText(this, "목표 금액이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}