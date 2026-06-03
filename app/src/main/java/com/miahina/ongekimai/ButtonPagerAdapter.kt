package com.miahina.ongekimai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class ButtonPagerAdapter(
    private val onTallyClick: () -> Unit,
    private val onGetJewelsClick: () -> Unit,
    private val onAnalyzerClick: () -> Unit,
    private val onScoreLogClick: () -> Unit,
    private val onNotificationTestClick: () -> Unit // 💡 追記：通知テスト用のコールバック
) : RecyclerView.Adapter<ButtonPagerAdapter.ButtonViewHolder>() {

    override fun getItemCount(): Int = 3 // 💡 「2」から「3」ページに変更[cite: 4]

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        // 💡 viewType（position）に応じて3つのレイアウトを切り替える[cite: 4]
        val layoutId = when (viewType) {
            0 -> R.layout.item_button_page1
                1 -> R.layout.item_button_page2
            else -> R.layout.item_button_page3 // 💡 追記：ページ3のレイアウトを指定
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        when (position) {
            0 -> {
                // 【1ページ目】[cite: 4]
                holder.itemView.findViewById<Button>(R.id.btnAnalyzer)?.setOnClickListener { onAnalyzerClick() }
                holder.itemView.findViewById<Button>(R.id.btnGetJewels)?.setOnClickListener { onGetJewelsClick() }
            }
            1 -> {
                // 【2ページ目】[cite: 4]
                holder.itemView.findViewById<Button>(R.id.btnTally)?.setOnClickListener { onTallyClick() }
                holder.itemView.findViewById<Button>(R.id.btnScoreLog)?.setOnClickListener { onScoreLogClick() }
            }
            2 -> {
                // 💡 【3ページ目】 新設したボタンに処理をバインド
                holder.itemView.findViewById<Button>(R.id.btnNotificationTest)?.setOnClickListener { onNotificationTestClick() }
            }
        }
    }

    class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}