package com.example.pnu_app_team17

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class GoalSettingActivity : AppCompatActivity() {
    private val goalAmounts = mutableMapOf<String, Int>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var goalContainer: LinearLayout
    private val userId by lazy { Auth.currentId(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_setting)

        goalContainer = findViewById(R.id.goalContainer)
        findViewById<TextView>(R.id.titleText).text = "목표 설정"

        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        showGoalInputs()

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveGoals()
        }
    }

    private fun showGoalInputs() {
        Category.values().forEach { category ->
            val row = layoutInflater.inflate(R.layout.item_goal_input, goalContainer, false)
            val categoryText = row.findViewById<TextView>(R.id.categoryText)
            val amountInput = row.findViewById<EditText>(R.id.amountInput)

            val catName = category.tag
            categoryText.text = catName
            amountInput.setText("0")

            amountInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val value = s.toString().replace(",", "").toIntOrNull() ?: 0
                    goalAmounts[catName] = value
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            goalContainer.addView(row)
            goalAmounts[catName] = 0
        }
    }

    private fun saveGoals() {
        if (userId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var totalGoal = 0
                val prefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
                val editor = prefs.edit()

                goalAmounts.forEach { (category, amount) ->
                    totalGoal += amount

                    // 🔴 Firestore 저장
                    val goalData = hashMapOf(
                        "id" to userId,
                        "category" to category,
                        "amount" to amount
                    )
                    db.collection("goals").add(goalData).await()

                    // 🔵 SharedPreferences에도 카테고리별 저장
                    editor.putInt(category, amount)
                }

                editor.putInt("total_goal", totalGoal)
                editor.apply() // ✅ 반드시 저장 적용

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GoalSettingActivity, "목표 금액이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@GoalSettingActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GoalSettingActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
