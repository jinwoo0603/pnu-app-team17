package com.example.pnu_app_team17

//소비내역 데이터를 담을 데이터 클래스
data class SobiItem(
    val documentId: String,
    val date: String,
    val category: String,
    val amount: Int
)
