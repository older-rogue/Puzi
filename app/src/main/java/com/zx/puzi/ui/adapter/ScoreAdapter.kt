package com.zx.puzi.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zx.puzi.R
import com.zx.puzi.local.FavoritesManager
import com.zx.puzi.model.Score

/**
 * 曲谱列表适配器
 */
class ScoreAdapter(
    private val isFavoritesFragment: Boolean = false,
    private val onItemClick: (Score) -> Unit
) :
    ListAdapter<Score, ScoreAdapter.ViewHolder>(ScoreDiffCallback()) {

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        
        private val titleTextView: TextView = itemView.findViewById(R.id.score_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.score_author)
        private val favoriteButton: ImageView = itemView.findViewById(R.id.favorite_button)

        fun bind(score: Score, isFavoritesFragment: Boolean, callback: () -> Unit) {
            titleTextView.text = score.title
            
            if (score.name.isNotEmpty()) {
                authorTextView.visibility = View.VISIBLE
                authorTextView.text = score.name
            } else {
                authorTextView.visibility = View.GONE
            }

            favoriteButton.isVisible = if (isFavoritesFragment) {
                favoriteButton.setImageResource(R.drawable.love)
                FavoritesManager.getInstance(itemView.context)
                    .isLove(score.url)
            } else {
                favoriteButton.setImageResource(R.drawable.favorite_white)
                FavoritesManager.getInstance(itemView.context)
                    .isFavorite(score.url)
            }

            titleTextView.setTextColor(
                if (score.isClick) {
                    itemView.context.resources.getColor(R.color.grey, null)
                } else {
                    itemView.context.resources.getColor(R.color.black, null)
                }
            )

            itemView.setOnClickListener {
                callback()
            }

        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_score, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isFavoritesFragment) {
            currentList[position].isClick = true
            notifyDataSetChanged()
            onItemClick(currentList[position])
        }
    }
}

/**
 * DiffUtil回调类，用于优化列表更新
 */
private class ScoreDiffCallback : DiffUtil.ItemCallback<Score>() {
    override fun areItemsTheSame(oldItem: Score, newItem: Score): Boolean {
        return oldItem.url == newItem.url
    }
    
    override fun areContentsTheSame(oldItem: Score, newItem: Score): Boolean {
        return oldItem == newItem
    }
} 