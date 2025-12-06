package cn.mareep.videofeeddemo.ui.main.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import cn.mareep.videofeeddemo.databinding.ItemVideoFeedBinding
import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity

class VideoFeedAdapter(
    private var items: List<VideoItemEntity>, private val listener: VideoInteractionListener
) : RecyclerView.Adapter<VideoFeedAdapter.VideoViewHolder>() {

    /**
     * 视频交互监听接口
     */
    interface VideoInteractionListener {
        /**
         * 视频区域点击
         */
        fun onVideoClick()

        /**
         * 点赞按钮点击
         */
        fun onLikeClick(videoId: String, position: Int)

        /**
         * 评论按钮点击
         */
        fun onCommentClick(videoId: String, position: Int)

        /**
         * 分享按钮点击
         */
        fun onShareClick(videoId: String, position: Int)

        /**
         * 作者信息点击（头像或名字）
         */
        fun onAuthorClick(videoId: String, position: Int)

        /**
         * 收藏按钮点击
         */
        fun onFavoriteClick(videoId: String, position: Int)

        /**
         * 关注按钮点击
         */
        fun onFollowClick(videoId: String, position: Int)
    }

    inner class VideoViewHolder(val binding: ItemVideoFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val handler = Handler(Looper.getMainLooper())
        private var isUserSeeking = false // 标记用户是否正在拖动进度条

        private val updateProgressAction = object : Runnable {
            override fun run() {
                updateProgress()
                handler.postDelayed(this, 200) // 每200ms更新一次
            }
        }

        init {
            // SeekBar 拖动监听
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // 用户开始拖动,暂停自动更新
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // 用户结束拖动,跳转到指定位置
                    seekBar?.let {
                        seekToPosition(it.progress)
                    }
                    isUserSeeking = false
                }
            })

            // 在 ViewHolder 创建时绑定点击事件
            binding.videoView.setOnClickListener {
                listener.onVideoClick()
            }

            // 点赞按钮点击
            binding.btnLike.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onLikeClick(items[position].id, position)
                }
            }

            // 评论按钮点击
            binding.btnComment.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCommentClick(items[position].id, position)
                }
            }

            // 底部快速评论点击
            binding.bottomComment.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCommentClick(items[position].id, position)
                }
            }

            // 分享按钮点击
            binding.btnShare.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onShareClick(items[position].id, position)
                }
            }

            // 作者名字点击
            binding.tvAuthorName.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAuthorClick(items[position].id, position)
                }
            }

            // 作者头像点击
            binding.ivAvatar.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAuthorClick(items[position].id, position)
                }
            }

            // 收藏按钮点击
            binding.btnFavorite.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFavoriteClick(items[position].id, position)
                }
            }

            // 关注按钮点击
            binding.btnFollow.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFollowClick(items[position].id, position)
                }
            }
        }

        fun bind(item: VideoItemEntity) {
            binding.tvAuthorName.text = item.authorName
            binding.tvVideoDesc.text = item.description
            binding.tvLikeCount.text = formatStats(item.likeCount)
            binding.tvCommentCount.text = formatStats(item.commentCount)
            binding.tvFavoriteCount.text = formatStats(item.favoriteCount)
        }

        /**
         * 把点赞、评论、收藏数据转换为相应字符串
         */
        private fun formatStats(count: Int): String {
            return when {
                count >= 10000 -> String.format("%.1fw", count / 10000.0)
                count >= 1000 -> String.format("%.1fk", count / 1000.0)
                else -> count.toString()
            }
        }

        /**
         * 更新播放和缓冲进度
         */
        private fun updateProgress() {
            // 如果用户正在拖动,不自动更新进度
            if (isUserSeeking) return

            val player = binding.videoView.player as? ExoPlayer ?: return

            val duration = player.duration
            if (duration > 0) {
                // 播放进度
                val position = player.currentPosition
                val progress = ((position * 100) / duration).toInt()
                binding.seekBar.progress = progress

                // 缓冲进度
                val bufferedPosition = player.bufferedPosition
                val bufferedProgress = ((bufferedPosition * 100) / duration).toInt()
                binding.seekBar.secondaryProgress = bufferedProgress
            }
        }

        /**
         * 跳转到指定进度位置
         */
        private fun seekToPosition(progress: Int) {
            val player = binding.videoView.player as? ExoPlayer ?: return
            val duration = player.duration
            if (duration > 0) {
                val targetPosition = (duration * progress) / 100
                player.seekTo(targetPosition)
            }
        }

        /**
         * 开始更新进度
         */
        fun startProgressUpdate() {
            handler.removeCallbacks(updateProgressAction)
            handler.post(updateProgressAction)
        }

        /**
         * 停止更新进度
         */
        fun stopProgressUpdate() {
            handler.removeCallbacks(updateProgressAction)
        }
    }

    // 初始化ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoFeedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VideoViewHolder(binding)
    }

    // 填充View
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewAttachedToWindow(holder: VideoViewHolder) {
        super.onViewAttachedToWindow(holder)
        // 视图附加到窗口时,开始更新进度
        holder.startProgressUpdate()
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // 停止更新进度
        holder.stopProgressUpdate()
        // 解绑 player,释放 PlayerView 资源
        holder.binding.videoView.player = null
    }

    override fun getItemCount(): Int = items.size

    /**
     * 更新视频列表数据
     */
    fun updateVideos(newItems: List<VideoItemEntity>) {
        val oldSize = items.size
        items = newItems
        // 通知只有新增的部分发生变化
        if (newItems.size > oldSize) {
            notifyItemRangeInserted(oldSize, newItems.size - oldSize)
        }
    }
}