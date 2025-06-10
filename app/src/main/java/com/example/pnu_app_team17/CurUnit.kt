package com.example.pnu_app_team17

//외화 종류를 나타내는 enum class
enum class CurUnit(val unit: String, val cur_nm: String) {
    AED("AED", "아랍에미리트 디르함"),
    ATS("ATS", "오스트리아 실링"),
    AUD("AUD", "호주 달러"),
    BEF("BEF", "벨기에 프랑"),
    BHD("BHD", "바레인 디나르"),
    CAD("CAD", "캐나다 달러"),
    CHF("CHF", "스위스 프랑"),
    CNY("CNY", "중국 위안"),
    DEM("DEM", "독일 마르크"),
    DKK("DKK", "덴마아크 크로네"),
    ESP("ESP(100)", "스페인 페세타"),
    EUR("EUR", "유로"),
    FIM("FIM", "핀란드 마르카"),
    FRF("FRF", "프랑스 프랑"),
    GBP("GBP", "영국 파운드"),
    HKD("HKD", "홍콩 달러"),
    IDR("IDR(100)", "인도네시아 루피아"),
    ITL("ITL(100)", "이태리 리라"),
    JPY("JPY(100)", "일본 옌"),
    KWD("KWD", "쿠웨이트 디나르"),
    MYR("MYR", "말레이지아 링기트"),
    NLG("NLG", "네델란드 길더"),
    NOK("NOK", "노르웨이 크로네"),
    NZD("NZD", "뉴질랜드 달러"),
    SAR("SAR", "사우디 리얄"),
    SEK("SEK", "스웨덴 크로나"),
    SGD("SGD", "싱가포르 달러"),
    THB("THB", "태국 바트"),
    USD("USD", "미국 달러"),
    XOF("XOF", "씨에프에이 프랑(비씨에이오)");
    override fun toString(): String = unit
}