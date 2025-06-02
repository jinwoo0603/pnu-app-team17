package com.example.pnu_app_team17

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class SpendingHistoryActivity : AppCompatActivity() {

    // 예시 데이터 (나중에 Firebase에서 불러올 예정)
    private val categories = listOf("교통비", "배달", "커피", "식비")
    private val goalAmounts = listOf(80000f, 60000f, 80000f, 200000f)
    private val actualAmounts = listOf(24000f, 60000f, 24000f, 120000f)

    private val historyList = listOf(
        SpendingItem(20000, "2025.04.12", "이삭 토스트", "식비"),
        SpendingItem(15000, "2025.04.11", "쿠팡 이츠", "배달"),
        SpendingItem(3000, "2025.04.11", "노스 커피", "커피")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_history)

        findViewById<TextView>(R.id.titleText).text = "소비 내역"

        // 막대 그래프 설정
        val barChart = findViewById<HorizontalBarChart>(R.id.barChart)
        val goalEntries = ArrayList<BarEntry>()
        val actualEntries = ArrayList<BarEntry>()

        for (i in categories.indices) {
            goalEntries.add(BarEntry(i.toFloat(), goalAmounts[i]))
            actualEntries.add(BarEntry(i.toFloat(), actualAmounts[i]))
        }

        val goalDataSet = BarDataSet(goalEntries, "목표").apply { color = ColorTemplate.COLORFUL_COLORS[0] }
        val actualDataSet = BarDataSet(actualEntries, "소비").apply { color = ColorTemplate.COLORFUL_COLORS[1] }

        val barData = BarData(goalDataSet, actualDataSet).apply {
            barWidth = 0.3f
        }
        barChart.data = barData
        barChart.groupBars(0f, 0.4f, 0.05f)
        barChart.description = Description().apply { text = "" }
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(categories)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setCenterAxisLabels(true)
        barChart.setFitBars(true)
        barChart.invalidate()

        // 리사이클러뷰 설정
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SpendingAdapter(historyList)
    }
}

// 소비 항목 데이터 클래스
data class SpendingItem(val amount: Int, val date: String, val store: String, val category: String)

// 어댑터 클래스 예시 (레이아웃과 함께 구현 필요)
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
        itemView.findViewById<TextView>(R.id.amountText).text = "% ,d원".format(item.amount)
        itemView.findViewById<TextView>(R.id.dateText).text = item.date
        itemView.findViewById<TextView>(R.id.storeText).text = item.store
        itemView.findViewById<TextView>(R.id.categoryText).text = item.category
    }
}
