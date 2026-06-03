package com.miahina.ongekimai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TallyAdapter(private val resultList: List<TallyResult>) :
    RecyclerView.Adapter<TallyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
        val tvCost: TextView = view.findViewById(R.id.tvCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tally_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = resultList[position]
        holder.tvDate.text = item.date
        holder.tvCount.text = "${item.count} 回"
        holder.tvCost.text = "${item.cost} 円"
    }

    override fun getItemCount(): Int = resultList.size
}