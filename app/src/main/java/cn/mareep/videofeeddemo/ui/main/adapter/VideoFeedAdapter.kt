package cn.mareep.videofeeddemo.ui.main.adapter

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.media3.common.Player
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
        private var playerListener: Player.Listener? = null

        // 横屏时控件的显示状态
        private var isControlsVisible = true

        // 自动隐藏控件的延迟任务
        private val hideControlsAction = Runnable {
            hideControls()
        }

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
                // 检查是否是横屏
                val isLandscape = binding.root.context.resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape) {
                    // 横屏时：切换控件显示/隐藏
                    toggleControls()
                } else {
                    // 竖屏时：切换播放/暂停
                    listener.onVideoClick()
                    // 点击视频区域时更新暂停图标显示状态
                    updatePauseIconVisibility()
                }
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

            // 检查当前屏幕方向，确保控件显示状态正确
            val isLandscape = binding.root.context.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                // 横屏：默认显示控件（小尺寸）
                isControlsVisible = true
                showControls()
            } else {
                // 竖屏：确保所有控件可见
                isControlsVisible = true
                binding.layoutInfo.visibility = View.VISIBLE
                binding.layoutActions?.visibility = View.VISIBLE
                binding.seekBar.visibility = View.VISIBLE
                // 取消任何自动隐藏任务
                handler.removeCallbacks(hideControlsAction)
            }
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

        /**
         * 更新暂停图标的可见性
         */
        private fun updatePauseIconVisibility() {
            val player = binding.videoView.player as? ExoPlayer ?: return
            // 如果播放器正在播放,隐藏暂停图标;如果暂停,显示暂停图标
            binding.imageView.visibility = if (player.playWhenReady) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

        /**
         * 设置播放器并添加状态监听
         */
        fun setPlayer(player: ExoPlayer?) {
            // 移除旧的监听器
            playerListener?.let {
                binding.videoView.player?.removeListener(it)
            }

            // 设置新的播放器
            binding.videoView.player = player

            // 添加新的监听器
            if (player != null) {
                playerListener = object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        // 播放状态改变时更新暂停图标
                        updatePauseIconVisibility()
                    }
                }
                player.addListener(playerListener!!)
                // 初始化暂停图标状态
                updatePauseIconVisibility()
            }
        }

        /**
         * 清理资源
         */
        fun cleanup() {
            playerListener?.let {
                binding.videoView.player?.removeListener(it)
            }
            playerListener = null
            // 清理自动隐藏任务
            handler.removeCallbacks(hideControlsAction)
        }

        /**
         * 切换控件显示/隐藏
         */
        private fun toggleControls() {
            if (isControlsVisible) {
                hideControls()
            } else {
                showControls()
                // 显示后3秒自动隐藏
                scheduleHideControls()
            }
        }

        /**
         * 显示控件
         */
        private fun showControls() {
            isControlsVisible = true
            binding.layoutInfo.visibility = View.VISIBLE
            binding.layoutActions?.visibility = View.VISIBLE
            binding.seekBar.visibility = View.VISIBLE
        }

        /**
         * 隐藏控件
         */
        private fun hideControls() {
            isControlsVisible = false
            binding.layoutInfo.visibility = View.GONE
            binding.layoutActions?.visibility = View.GONE
            binding.seekBar.visibility = View.GONE
        }

        /**
         * 安排自动隐藏控件（3秒后）
         */
        private fun scheduleHideControls() {
            handler.removeCallbacks(hideControlsAction)
            handler.postDelayed(hideControlsAction, 3000)
        }

        /**
         * 取消自动隐藏
         */
        private fun cancelHideControls() {
            handler.removeCallbacks(hideControlsAction)
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
        // 清理监听器
        holder.cleanup()
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