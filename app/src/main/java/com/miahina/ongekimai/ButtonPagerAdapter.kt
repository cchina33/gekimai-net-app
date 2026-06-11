package com.miahina.ongekimai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miahina.ongekimai.databinding.ItemButtonPage1Binding
import com.miahina.ongekimai.databinding.ItemButtonPage2Binding
import com.miahina.ongekimai.databinding.ItemButtonPage3Binding

class ButtonPagerAdapter(
    private val onTallyClick: () -> Unit,
    private val onGetJewelsClick: () -> Unit,
    private val onAnalyzerClick: () -> Unit,
    private val onScoreLogClick: () -> Unit,
    private val onSelectiveScreenshotClick: () -> Unit,
    private val onOverPrintClick: () -> Unit,
    private val onDaydreamCalcClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = 3

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> Page1ViewHolder(ItemButtonPage1Binding.inflate(inflater, parent, false))
            1 -> Page2ViewHolder(ItemButtonPage2Binding.inflate(inflater, parent, false))
            else -> Page3ViewHolder(ItemButtonPage3Binding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is Page1ViewHolder -> {
                holder.binding.btnAnalyzer.setOnClickListener { onAnalyzerClick() }
                holder.binding.btnGetJewels.setOnClickListener { onGetJewelsClick() }
            }
            is Page2ViewHolder -> {
                holder.binding.btnTally.setOnClickListener { onTallyClick() }
                holder.binding.btnScoreLog.setOnClickListener { onScoreLogClick() }
            }
            is Page3ViewHolder -> {
                holder.binding.btnSelectiveScreenshot.setOnClickListener { onSelectiveScreenshotClick() }
                holder.binding.btnOverPrint.setOnClickListener { onOverPrintClick() }
                holder.binding.btnDaydreamCalc.setOnClickListener { onDaydreamCalcClick() }
            }
        }
    }

    class Page1ViewHolder(val binding: ItemButtonPage1Binding) : RecyclerView.ViewHolder(binding.root)
    class Page2ViewHolder(val binding: ItemButtonPage2Binding) : RecyclerView.ViewHolder(binding.root)
    class Page3ViewHolder(val binding: ItemButtonPage3Binding) : RecyclerView.ViewHolder(binding.root)
}
