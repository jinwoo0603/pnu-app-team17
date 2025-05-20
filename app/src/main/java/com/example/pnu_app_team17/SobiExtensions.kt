package com.example.pnu_app_team17

import java.time.LocalDate
import java.time.format.DateTimeFormatter

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