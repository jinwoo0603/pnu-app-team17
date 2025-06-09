package com.example.pnu_app_team17

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

//소비 내역 데이터에 대한 유틸리티 함수 모음

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

// 날짜 기준 정렬
fun List<SobiItem>.sortedByDate(descending: Boolean = true): List<SobiItem> {
    return this.sortedBy { LocalDate.parse(it.date, dateFormatter) }
        .let { if (descending) it.reversed() else it }
}

// 특정 연-월("yyyy-MM") 기준 필터링
fun List<SobiItem>.filterByMonth(month: String): List<SobiItem> {
    return this.filter { it.date.startsWith(month) }
}

// 카테고리별 그룹화
fun List<SobiItem>.groupByCategory(): Map<String, List<SobiItem>> {
    return this.groupBy { it.category }
}

// 총 소비 금액
fun List<SobiItem>.totalAmount(): Int {
    return this.sumOf { it.amount }
}

// 카테고리별 평균 금액
fun List<SobiItem>.averageAmountByCategory(): Map<String, Double> {
    return this.groupByCategory().mapValues { (_, items) ->
        items.map { it.amount }.average()
    }
}

// 총액 기준 상위 소비 카테고리 정렬
fun List<SobiItem>.topCategories(limit: Int = 3): List<Pair<String, Int>> {
    return this.groupByCategory()
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key to it.value }
}

// CSV 형식의 문자열로 반환
fun List<SobiItem>.toCsvString(): String {
    if (isEmpty()) return ""

    val header = "date,category,amount"
    val rows = this.map { "${it.date},${it.category},${it.amount}" }
    return (listOf(header) + rows).joinToString("\n")
}

// 카테고리별 소비 비율을 Map형태로 반환
fun List<SobiItem>.categoryProportions(): Map<String, Double> {
    val total = this.totalAmount().toDouble()
    if (total == 0.0) return emptyMap()

    return this.groupBy { it.category }
        .mapValues { (_, items) ->
            val categorySum = items.sumOf { it.amount }
            (categorySum / total * 100.0)
        }
}

// 카테고리별 소비 비율을 파이 차트로 시각화
fun List<SobiItem>.showPieChart(pieChart: PieChart) {
    val proportions = this.categoryProportions()
    if (proportions.isEmpty()) {
        pieChart.clear()
        pieChart.centerText = "데이터 없음"
        return
    }

    val entries = proportions.map { (category, percent) ->
        PieEntry(percent.toFloat(), category)
    }

    val dataSet = PieDataSet(entries, "카테고리별 비율").apply {
        sliceSpace = 2f
        valueTextSize = 12f
        colors = listOf(
            Color.parseColor("#FFA726"), // 주황
            Color.parseColor("#66BB6A"), // 녹색
            Color.parseColor("#42A5F5"), // 파랑
            Color.parseColor("#EF5350"), // 빨강
            Color.parseColor("#AB47BC")  // 보라
        )
    }

    val data = PieData(dataSet)

    pieChart.apply {
        this.data = data
        description.isEnabled = false
        isDrawHoleEnabled = true
        setEntryLabelColor(Color.BLACK)
        setUsePercentValues(true)
        centerText = "소비 비율"
        invalidate() // 차트 새로고침
    }
}

fun List<SobiItem>.toTFLiteInput(
    seqLen: Int = 5,
    amountMin: Float = 0f,
    amountMax: Float = 50000f
): Array<Array<FloatArray>> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    val seq = this.takeLast(seqLen).padStart(seqLen)

    val input = Array(1) { Array(seqLen) { FloatArray(5) } }

    for (i in seq.indices) {
        val item = seq[i]
        val cal = Calendar.getInstance()
        try {
            cal.time = dateFormat.parse(item.date) ?: Date()
        } catch (_: Exception) {
            cal.time = Date()
        }

        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1

        val normAmount = ((item.amount.toFloat() - amountMin) / (amountMax - amountMin)).coerceIn(0f, 1f)
        val categoryIdx = Category.values().indexOfFirst { it.tag == item.category }.takeIf { it >= 0 } ?: 0
        val normDow = dow.toFloat() / 6f
        val normDay = day.toFloat() / 31f
        val normMonth = month.toFloat() / 12f

        input[0][i][0] = normAmount
        input[0][i][1] = categoryIdx.toFloat()
        input[0][i][2] = normDow
        input[0][i][3] = normDay
        input[0][i][4] = normMonth
    }

    return input
}

fun List<SobiItem>.padStart(size: Int, filler: SobiItem? = null): List<SobiItem> {
    val delta = size - this.size
    val fillerItem = filler ?: this.firstOrNull() ?: SobiItem("dummy", "2025-01-01", "기타", 0)
    return List(delta.coerceAtLeast(0)) { fillerItem } + this.takeLast(size)
}
