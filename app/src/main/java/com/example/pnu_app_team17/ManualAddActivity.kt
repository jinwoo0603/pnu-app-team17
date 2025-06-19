package com.example.pnu_app_team17

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class ManualAddActivity : AppCompatActivity() {

    private lateinit var dateButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var addButton: Button

    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_add)

        dateButton = findViewById(R.id.button_date_picker)
        categorySpinner = findViewById(R.id.spinner_category)
        amountInput = findViewById(R.id.edit_amount)
        addButton = findViewById(R.id.button_submit)

        // 날짜 버튼 초기 텍스트 설정
        dateButton.text = selectedDate.format(DateTimeFormatter.ISO_DATE)

        // 카테고리 스피너 초기화
        val categories = Category.values().map { it.tag }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // 날짜 선택기
        dateButton.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    dateButton.text = selectedDate.format(DateTimeFormatter.ISO_DATE)
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 소비 추가 버튼
        addButton.setOnClickListener {
            val category = categorySpinner.selectedItem.toString()
            val amount = amountInput.text.toString().toIntOrNull()

            if (amount == null || amount <= 0) {
                Toast.makeText(this, "금액을 올바르게 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Sobi.add(this, selectedDate, category, amount)

            // MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
