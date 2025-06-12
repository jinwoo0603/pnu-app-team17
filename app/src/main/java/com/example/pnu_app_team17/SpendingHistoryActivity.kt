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
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.components.XAxis
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

            val historyList = spendingItems.map {
                val formattedDate = LocalDate.parse(it.date, dateFormatter).toString()
                SpendingItem(it.amount, formattedDate, it.category)
            }
            recyclerView.adapter = SpendingAdapter(historyList)

            val actualPerCategory: Map<String, Float> = spendingItems
                .groupBy { it.category }
                .mapValues { (_, items) -> items.sumOf { it.amount }.toFloat() }

            val db = FirebaseFirestore.getInstance()
            val userId = Auth.currentId(this@SpendingHistoryActivity) ?: return@launch
            val goalSnapshot = db.collection("goals")
                .whereEqualTo("id", userId)
                .get().await()

            val goalPerCategory: Map<String, Float> = goalSnapshot.documents.associate {
                val cat = it.getString("category") ?: ""
                val amount = it.getLong("amount")?.toFloat() ?: 0f
                cat to amount
            }

            // 소비 데이터만 있는 경우에도 그래프가 그려지도록 조건 제거
            val allCategories = (actualPerCategory.keys + goalPerCategory.keys).distinct()
                .sorted()

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
                barWidth = 0.35f
            }

            barChart.apply {
                data = barData
                description = Description().apply { text = "" }
                setFitBars(true)
                setScaleEnabled(false)
                setPinchZoom(false)
                animateY(1000)

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(categoryLabels)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    labelCount = categoryLabels.size
                    axisMinimum = -0.5f
                    axisMaximum = categoryLabels.size - 0.5f
                    setCenterAxisLabels(true)
                }

                axisLeft.axisMinimum = 0f
                axisRight.isEnabled = false

                groupBars(0f, 0.4f, 0.05f)
                invalidate()
            }
        }
    }
}

// 데이터 클래스
data class SpendingItem(val amount: Int, val date: String, val category: String)

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
        itemView.findViewById<TextView>(R.id.categoryText).text = item.category
    }
}
