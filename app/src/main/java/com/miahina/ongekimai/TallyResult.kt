package com.miahina.ongekimai

// JavaScript側の { date: "...", count: 3, cost: 300 } に対応するクラス
data class TallyResult(
    val date: String,
    val count: Int,
    val cost: Int,
    val totalGP: Int? = null // オンゲキのみ存在する項目（なくてもよいようにnull許容）
)