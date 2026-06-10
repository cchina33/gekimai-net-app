package com.miahina.ongekimai

/**
 * 親密度解析結果のデータモデル
 */
data class IntimacyResult(
    val idx: String,        // キャラクターID
    val friendly: Int,      // 現在の親密度レベル
    val next: Int           // 次のレベルまでの残り
)

data class OverPrintData(
    val friendly_data: List<IntimacyResult>,
    val money_data: String,
    val item_big: Int = 0,
    val item_mid: Int = 0,
    val item_small: Int = 0
)

/**
 * オンゲキ-NETのidxと名前の正しいマッピング
 * ログデータに基づき修正済み
 */
object CharacterMapper {
    private val nameMap = mapOf(
        "1000" to "星咲 あかり",
        "1001" to "藤沢 柚子",
        "1002" to "三角 葵",
        "1003" to "高瀬 梨緒",
        "1004" to "結城 莉玖",
        "1005" to "藍原 椿",
        "1006" to "早乙女 彩華",
        "1007" to "桜井 春菜",
        "1008" to "九條 楓",
        "1009" to "柏木 咲姫",
        "1010" to "井之原 小星",
        "1011" to "逢坂 茜",
        "1012" to "珠洲島 有栖",
        "1013" to "柏木 美亜",
        "1014" to "日向 千夏",
        "1015" to "東雲 つむぎ",
        "1016" to "皇城 セツナ"
    )

    fun getName(idx: String): String = nameMap[idx] ?: "Unknown ($idx)"
}
