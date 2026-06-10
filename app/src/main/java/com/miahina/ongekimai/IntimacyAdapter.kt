package com.miahina.ongekimai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miahina.ongekimai.databinding.ItemIntimacyRowBinding

import java.util.Locale

class IntimacyAdapter(private var items: List<IntimacyResult>) :
    RecyclerView.Adapter<IntimacyAdapter.ViewHolder>() {

    private var goalLevel: Int = 800
    private var heldBig: Int = 0
    private var heldMid: Int = 0
    private var heldSmall: Int = 0

    class ViewHolder(val binding: ItemIntimacyRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIntimacyRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.binding.tvCharName.text = CharacterMapper.getName(item.idx)
        holder.binding.tvCurrentLevel.text = item.friendly.toString()
        holder.binding.tvGoalLevel.text = goalLevel.toString()

        if (item.friendly >= goalLevel) {
            holder.binding.tvRequiredItems.text = "達成！"
            holder.binding.tvTotalCost.text = "0"
        } else {
            val reqPoints = IntimacyCalculator.calculateRequiredPoints(item.friendly, goalLevel)
            val reqItems = IntimacyCalculator.calculateRequiredItems(reqPoints)
            
            val itemsStr = buildString {
                if (reqItems.big > 0) {
                    val diff = (reqItems.big - heldBig).coerceAtLeast(0)
                    append("大${reqItems.big}")
                    if (heldBig > 0) append("($diff)")
                    append(" ")
                }
                if (reqItems.mid > 0) {
                    val diff = (reqItems.mid - heldMid).coerceAtLeast(0)
                    append("中${reqItems.mid}")
                    if (heldMid > 0) append("($diff)")
                    append(" ")
                }
                if (reqItems.small > 0) {
                    val diff = (reqItems.small - heldSmall).coerceAtLeast(0)
                    append("小${reqItems.small}")
                    if (heldSmall > 0) append("($diff)")
                }
            }.trim()
            
            holder.binding.tvRequiredItems.text = itemsStr
            holder.binding.tvTotalCost.text = String.format(Locale.getDefault(), "%,d", reqItems.cost)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<IntimacyResult>, goal: Int = 800, big: Int = 0, mid: Int = 0, small: Int = 0) {
        this.goalLevel = goal
        this.heldBig = big
        this.heldMid = mid
        this.heldSmall = small
        // 親密度が高い順にソート
        items = newItems.sortedByDescending { it.friendly }
        notifyDataSetChanged()
    }
}
