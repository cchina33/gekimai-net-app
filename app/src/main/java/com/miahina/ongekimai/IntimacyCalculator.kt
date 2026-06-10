package com.miahina.ongekimai

import kotlin.math.ceil

/**
 * 親密度計算ロジック
 * 参照: https://mel225.sakura.ne.jp/friendly/
 */
object IntimacyCalculator {

    /**
     * 指定レベルから次のレベルに上がるために必要なポイントを返す
     */
    fun getPointsForLevel(level: Int): Double {
        val hundred = level / 100
        val tens = (level % 100) / 10
        
        val base = when {
            level >= 1000 -> 31.2
            (hundred == 6 || hundred == 8) -> 22.8
            (hundred == 7 || hundred == 9) -> 31.2
            hundred in 0..5 -> 6.0 + 1.2 * hundred
            else -> 31.2
        }
        
        return base * (tens + 1)
    }

    /**
     * 現在のレベルから目標レベルまでに必要な合計ポイントを計算する
     */
    fun calculateRequiredPoints(currentLevel: Int, goalLevel: Int): Double {
        if (currentLevel >= goalLevel) return 0.0
        var total = 0.0
        for (lv in currentLevel until goalLevel) {
            total += getPointsForLevel(lv)
        }
        return total
    }

    /**
     * 必要なポイントを最小コストのアイテム数に変換する
     * 大: 200pt / 20000マニー
     * 中: 20pt / 2500マニー
     * 小: 6pt / 900マニー
     */
    fun calculateRequiredItems(points: Double): ItemRequirement {
        val p = ceil(points).toInt()
        
        var big = p / 200
        val rem = p % 200
        
        var mid = rem / 20
        var small = ceil((rem % 20) / 6.0).toInt()
        
        // 最適化: 小3個(2700)より中1個(2500)の方が安い
        if (small >= 3) {
            mid++
            small = 0
        }
        
        // 最適化: 中8個(20000)と大1個(20000)は同額だが、大の方がポイントが多い
        if (mid >= 8) {
            big++
            mid = 0
        }
        
        val cost = (big * 20000) + (mid * 2500) + (small * 900)
        return ItemRequirement(big, mid, small, cost)
    }

    data class ItemRequirement(
        val big: Int,
        val mid: Int,
        val small: Int,
        val cost: Int
    )
}
