package cn.mareep.videofeeddemo.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.mareep.videofeeddemo.databinding.ItemVideoFeedBinding
import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity

class VideoFeedAdapter(private val items: List<VideoItemEntity>) :
    RecyclerView.Adapter<VideoFeedAdapter.VideoViewHolder>() {

    class VideoViewHolder(val binding: ItemVideoFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItemEntity) {
            binding.tvAuthorName.text = item.authorName
            binding.tvVideoDesc.text = item.description
            binding.tvLikeCount.text = item.likeCount
            binding.tvCommentCount.text = item.commentCount
            binding.tvFavoriteCount.text = item.favoriteCount
        }
    }

    // 初始化ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoFeedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    // 填充View
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // 解绑 player,释放 PlayerView 资源
        holder.binding.videoView.player = null
    }

    override fun getItemCount(): Int = items.size
}