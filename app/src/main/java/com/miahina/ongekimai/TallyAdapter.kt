package com.miahina.ongekimai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miahina.ongekimai.databinding.ItemTallyResultBinding

class TallyAdapter(private val resultList: List<TallyResult>) :
    RecyclerView.Adapter<TallyAdapter.ViewHolder>() {

    // ViewBinding を使用するように ViewHolder を変更
    class ViewHolder(val binding: ItemTallyResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTallyResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = resultList[position]
        holder.binding.tvDate.text = item.date
        holder.binding.tvCount.text = "${item.count} 回"
        holder.binding.tvCost.text = "${item.cost} 円"
    }

    override fun getItemCount(): Int = resultList.size
}
