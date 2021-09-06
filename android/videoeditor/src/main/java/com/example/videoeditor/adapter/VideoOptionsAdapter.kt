/*
 * The Metronome by Soundbrenner
 *
 * Copyright (c) 2021 Soundbrenner Limited. All rights reserved.
 */

package com.example.videoeditor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.videoeditor.databinding.ItemVideoOptionBinding
import com.example.videoeditor.models.VideoOptionModel

class VideoOptionsAdapter(private val dataList: List<VideoOptionModel>) :
    RecyclerView.Adapter<VideoOptionsAdapter.MyHolder>() {
    var onItemClicked: ((VideoOptionModel, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val itemBinding =
            ItemVideoOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item: VideoOptionModel = dataList[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemClicked?.invoke(item, position)
        }
    }

    override fun getItemCount(): Int = dataList.size

    class MyHolder(private val itemBinding: ItemVideoOptionBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: VideoOptionModel) {
            itemBinding.tvTitle.text = item.title
            itemBinding.imgIcon.setImageResource(item.icon)
            itemBinding.loutContent.setOnClickListener {

            }
        }
    }
}