package com.example.pnu_app_team17

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExchangeActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var editAmount: EditText
    private lateinit var buttonConvert: Button
    private lateinit var textResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange)

        spinner = findViewById(R.id.spinner_currency)
        editAmount = findViewById(R.id.edit_amount)
        buttonConvert = findViewById(R.id.button_convert)
        textResult = findViewById(R.id.text_result)

        val currencyList = CurUnit.values().toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyList.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        buttonConvert.setOnClickListener {
            val amountText = editAmount.text.toString()

            if (amountText.isBlank()) {
                Toast.makeText(this, "외화 금액을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "유효한 숫자를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCurrency = CurUnit.values()[spinner.selectedItemPosition]

            lifecycleScope.launch {
                val today = LocalDate.now()
                try {
                    val won = try {
                        Exchange.get(this@ExchangeActivity, today, selectedCurrency, amount)
                    } catch (e: Exception) {
                        Log.w("ExchangeActivity", "오늘 환율 실패, 전날 fallback 시도: ${e.message}")
                        Exchange.get(this@ExchangeActivity, today.minusDays(1), selectedCurrency, amount)
                    }

                    textResult.text = "원화 금액: ${won}원"
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ExchangeActivity, "환율 정보를 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
