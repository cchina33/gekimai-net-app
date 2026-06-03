package com.miahina.ongekimai

import android.webkit.JavascriptInterface
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

class WebAppInterface(private val activity: MainActivity) {

    /**
     * プレイ集計結果
     */
    @JavascriptInterface
    fun sendResults(jsonResult: String) {

        activity.runOnUiThread {
            try {
                val gson = Gson()

                val listType =
                    object : TypeToken<List<TallyResult>>() {}.type

                val tallyList: List<TallyResult> =
                    gson.fromJson(jsonResult, listType)

                if (tallyList.isEmpty()) {

                    AlertDialog.Builder(activity)
                        .setMessage("プレイデータが見つかりませんでした。")
                        .setPositiveButton("OK", null)
                        .show()

                    return@runOnUiThread
                }

                val totalCount = tallyList.sumOf { it.count }
                val totalCost = tallyList.sumOf { it.cost }

                val dialogView =
                    LayoutInflater.from(activity)
                        .inflate(
                            R.layout.dialog_tally_report,
                            null
                        )

                val tvTotalReport =
                    dialogView.findViewById<TextView>(
                        R.id.tvTotalReport
                    )

                tvTotalReport.text =
                    "総プレイ回数: ${totalCount}回\n総使用金額: ${totalCost}円"

                val recyclerView =
                    dialogView.findViewById<RecyclerView>(
                        R.id.recyclerViewTally
                    )

                recyclerView.layoutManager =
                    LinearLayoutManager(activity)

                recyclerView.adapter =
                    TallyAdapter(tallyList)

                recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        activity,
                        DividerItemDecoration.VERTICAL
                    )
                )

                AlertDialog.Builder(activity)
                    .setTitle("プレイ集計レポート")
                    .setView(dialogView)
                    .setPositiveButton("閉じる", null)
                    .show()

            } catch (e: Exception) {

                Log.e(
                    "MAL_ANALYZER",
                    "sendResults Error",
                    e
                )
            }
        }
    }

    /**
     * ジュエル・しずく取得結果
     */
    @JavascriptInterface
    fun sendJewelResults(jsonResult: String) {

        activity.runOnUiThread {

            try {

                val gson = Gson()

                val listType =
                    object : TypeToken<List<String>>() {}.type

                val jewelList: List<String> =
                    gson.fromJson(jsonResult, listType)

                val content =
                    jewelList.joinToString("\n")

                AlertDialog.Builder(activity)
                    .setTitle("💎 ジュエル・しずく取得結果")
                    .setMessage(content)
                    .setPositiveButton("OK", null)
                    .show()

            } catch (e: Exception) {

                Log.e(
                    "MAL_ANALYZER",
                    "sendJewelResults Error",
                    e
                )

                AlertDialog.Builder(activity)
                    .setTitle("エラー")
                    .setMessage("データの解析に失敗しました")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    /**
     * あならいざもどき画像受信
     */
    @JavascriptInterface
    fun showPreviewImage(imageData: String) {

        Log.d(
            "MAL_ANALYZER",
            "showPreviewImage 呼び出し"
        )

        Log.d(
            "MAL_ANALYZER",
            imageData.take(100)
        )

        activity.runOnUiThread {

            try {

                when {

                    imageData.startsWith("data:image") -> {

                        Log.d(
                            "MAL_ANALYZER",
                            "Base64画像受信"
                        )

                        activity.showImagePreviewDialog(
                            imageData
                        )
                    }

                    imageData.startsWith("blob:") -> {

                        Log.d(
                            "MAL_ANALYZER",
                            "Blob画像受信"
                        )

                        activity.handleGeneratedImage(
                            imageData
                        )
                    }

                    else -> {

                        Log.d(
                            "MAL_ANALYZER",
                            "未知形式"
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "MAL_ANALYZER",
                    "showPreviewImage Error",
                    e
                )
            }
        }
    }
}