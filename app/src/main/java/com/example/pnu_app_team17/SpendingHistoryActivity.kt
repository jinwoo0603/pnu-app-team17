package com.example.pnu_app_team17

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SpendingHistoryActivity : AppCompatActivity() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_history)

        findViewById<TextView>(R.id.titleText).text = "소비 내역"

        val barChart = findViewById<HorizontalBarChart>(R.id.barChart)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val spendingItems = Sobi.get(this@SpendingHistoryActivity)

            // 🟦 리사이클러뷰 표시
            val historyList = spendingItems.map {
                val formattedDate = LocalDate.parse(it.date, dateFormatter).toString()
                SpendingItem(it.amount, formattedDate, "가게명 미지정", it.category)
            }
            recyclerView.adapter = SpendingAdapter(historyList)

            // 🟥 카테고리별 실제 소비 계산
            val actualPerCategory: Map<String, Float> = spendingItems
                .groupBy { it.category }
                .mapValues { (_, items) -> items.sumOf { it.amount }.toFloat() }

            // 🟨 목표 소비 SharedPreferences에서 불러오기
            val prefs = getSharedPreferences("goal_prefs", MODE_PRIVATE)
            val goalPerCategory: Map<String, Float> = Category.values().associate {
                val amount = prefs.getInt(it.tag, 0)
                it.tag to amount.toFloat()
            }
            // 🔍 SharedPreferences에서 불러온 목표 금액 로그 확인
            goalPerCategory.forEach { (category, amount) ->
                android.util.Log.d("GOAL_PREFS", "카테고리: $category, 목표 금액: $amount")
            }


            // 🟩 모든 카테고리 모음
            val allCategories = (actualPerCategory.keys + goalPerCategory.keys).distinct()

            // 그래프 데이터 만들기
            val goalEntries = ArrayList<BarEntry>()
            val actualEntries = ArrayList<BarEntry>()
            val categoryLabels = ArrayList<String>()

            allCategories.forEachIndexed { index, category ->
                val actual = actualPerCategory[category] ?: 0f
                val goal = goalPerCategory[category] ?: 0f
                goalEntries.add(BarEntry(index.toFloat(), goal))
                actualEntries.add(BarEntry(index.toFloat(), actual))
                categoryLabels.add(category)
            }

            val goalDataSet = BarDataSet(goalEntries, "목표").apply {
                color = ColorTemplate.COLORFUL_COLORS[0]
            }
            val actualDataSet = BarDataSet(actualEntries, "소비").apply {
                color = ColorTemplate.COLORFUL_COLORS[1]
            }

            val barData = BarData(goalDataSet, actualDataSet).apply {
                barWidth = 0.3f
            }

            barChart.data = barData
            barChart.groupBars(0f, 0.4f, 0.05f)
            barChart.description = Description().apply { text = "" }
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(categoryLabels)
            barChart.xAxis.granularity = 1f
            barChart.xAxis.setCenterAxisLabels(true)
            barChart.setFitBars(true)
            barChart.invalidate()
        }
    }
}

// 소비 항목 데이터 클래스
data class SpendingItem(val amount: Int, val date: String, val store: String, val category: String)

// 어댑터
class SpendingAdapter(private val items: List<SpendingItem>) : RecyclerView.Adapter<SpendingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpendingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spending, parent, false)
        return SpendingViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpendingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

class SpendingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(item: SpendingItem) {
        itemView.findViewById<TextView>(R.id.amountText).text = "%,d원".format(item.amount)
        itemView.findViewById<TextView>(R.id.dateText).text = item.date
        itemView.findViewById<TextView>(R.id.storeText).text = item.store
        itemView.findViewById<TextView>(R.id.categoryText).text = item.category
    }
}
