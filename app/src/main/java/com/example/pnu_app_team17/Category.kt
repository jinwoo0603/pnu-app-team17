package com.example.pnu_app_team17

// 소비 카테고리
enum class Category(val tag: String) {
    MEAL("식비"),
    CAFE("카페"),
    TRAFFIC("교통비"),
    SHOP("쇼핑"),
    OTHER("기타");
    companion object {
        fun fromTag(tag: String): Category? = values().find { it.tag == tag }
        fun fromIndex(idx: Int): Category = values().getOrElse(idx) { OTHER }
    }
}