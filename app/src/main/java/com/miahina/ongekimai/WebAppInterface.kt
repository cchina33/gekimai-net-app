package com.miahina.ongekimai

import android.util.Log
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.miahina.ongekimai.databinding.DialogTallyReportBinding

class WebAppInterface(private val activity: MainActivity) {

    /**
     * プレイ集計結果
     */
    @JavascriptInterface
    fun sendResults(jsonResult: String) {
        activity.runOnUiThread {
            try {
                val gson = Gson()
                val listType = object : TypeToken<List<TallyResult>>() {}.type
                val tallyList: List<TallyResult> = gson.fromJson(jsonResult, listType)

                if (tallyList.isEmpty()) {
                    AlertDialog.Builder(activity).setMessage("プレイデータが見つかりませんでした。").setPositiveButton("OK", null).show()
                    return@runOnUiThread
                }

                val binding = DialogTallyReportBinding.inflate(LayoutInflater.from(activity))
                val totalCount = tallyList.sumOf { it.count }
                val totalCost = tallyList.sumOf { it.cost }
                binding.tvTotalReport.text = activity.getString(R.string.total_report_format, totalCount, totalCost)

                binding.recyclerViewTally.layoutManager = LinearLayoutManager(activity)
                binding.recyclerViewTally.adapter = TallyAdapter(tallyList)
                binding.recyclerViewTally.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

                AlertDialog.Builder(activity).setTitle("プレイ集計レポート").setView(binding.root).setPositiveButton("閉じる", null).show()
            } catch (e: Exception) {
                Log.e("MAL_ANALYZER", "sendResults Error", e)
            }
        }
    }

    /**
     * 親密度解析結果（Over Print）の受け取り
     */
    @JavascriptInterface
    fun receiveOverPrintData(jsonResult: String) {
        activity.runOnUiThread {
            try {
                val gson = Gson()
                val data: OverPrintData = gson.fromJson(jsonResult, OverPrintData::class.java)
                
                // MainActivityのメソッドを呼び出してDrawerの内容を更新
                activity.updateIntimacyDrawer(data)
                
                // 解析完了を通知してDrawerを開く
                activity.showToast("解析が完了しました。サイドメニューをご確認ください。")
                activity.openIntimacyPage()
                
            } catch (e: Exception) {
                Log.e("OVER_PRINT", "解析データの処理に失敗", e)
                activity.showToast("解析データの処理に失敗しました")
            }
        }
    }

    @JavascriptInterface
    fun sendJewelResults(jsonResult: String) {
        activity.runOnUiThread {
            try {
                val gson = Gson()
                val listType = object : TypeToken<List<String>>() {}.type
                val jewelList: List<String> = gson.fromJson(jsonResult, listType)
                val content = jewelList.joinToString("\n")
                AlertDialog.Builder(activity).setTitle("💎 ジュエル・しずく取得結果").setMessage(content).setPositiveButton("OK", null).show()
            } catch (e: Exception) {
                Log.e("MAL_ANALYZER", "sendJewelResults Error", e)
            }
        }
    }

    @JavascriptInterface
    fun receiveJewelsForCalc(jewels: Int) {
        activity.runOnUiThread {
            activity.updateDaydreamJewels(jewels)
        }
    }

    @JavascriptInterface
    fun showPreviewImage(imageData: String) {
        activity.runOnUiThread {
            if (imageData.startsWith("data:image")) activity.showImagePreviewDialog(imageData)
            else if (imageData.startsWith("blob:")) activity.handleGeneratedImage(imageData)
        }
    }

    /**
     * JavaScript側からアプリのトースト通知を呼び出す
     */
    @JavascriptInterface
    fun showToast(msg: String) {
        activity.runOnUiThread {
            activity.showToast(msg)
        }
    }

    /**
     * JavaScript側でのエラーをAndroid StudioのLogcatに出力する（デバッグ用）
     */
    @JavascriptInterface
    fun logError(msg: String) {
        Log.e("JS_SCRAPE_ERROR", msg)
    }
}
