package cn.mareep.videofeeddemo.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity
import cn.mareep.videofeeddemo.data.repository.VideoRepository
import cn.mareep.videofeeddemo.utils.analytics.VideoPerformanceTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoRepository: VideoRepository, application: Application
) : AndroidViewModel(application) {

    private val _videoList = MutableLiveData<List<VideoItemEntity>>()
    val videoList: LiveData<List<VideoItemEntity>> = _videoList
    private val _currentPosition = MutableLiveData(0)

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var player: ExoPlayer? = null
    private var currentPage = 0
    private val pageSize = 10
    private var isLastPage = false

    // 性能监控追踪器
    private var performanceTracker: VideoPerformanceTracker? = null

    init {
        loadInitialVideos()
    }

    /**
     * 初始化播放器
     */
    fun initializePlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(getApplication()).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val stateString = when (playbackState) {
                            ExoPlayer.STATE_IDLE -> "STATE_IDLE"
                            ExoPlayer.STATE_BUFFERING -> "STATE_BUFFERING"
                            ExoPlayer.STATE_READY -> "STATE_READY"
                            ExoPlayer.STATE_ENDED -> "STATE_ENDED"
                            else -> "UNKNOWN_STATE"
                        }
                        Log.d("ExoPlayerDebug", "onPlaybackStateChanged: $stateString")
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        Log.d(
                            "ExoPlayerDebug",
                            "onPlayWhenReadyChanged: $playWhenReady, reason: $reason"
                        )
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("ExoPlayerDebug", "onPlayerError: ", error)
                    }
                })
            }
        }
        return player!!
    }

    /**
     * 获取播放器实例
     */
    fun getPlayer(): ExoPlayer? = player

    /**
     * 加载初始视频数据
     */
    private fun loadInitialVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 0
            isLastPage = false

            videoRepository.getVideos(currentPage, pageSize).fold(onSuccess = { videos ->
                _videoList.value = videos
                isLastPage = videos.size < pageSize
                currentPage++
                Log.d("MainViewModel", "初始加载成功: ${videos.size} 条视频")
            }, onFailure = { error ->
                Log.e("MainViewModel", "初始加载失败", error)
                _videoList.value = emptyList()
            })
            _isLoading.value = false
        }
    }

    /**
     * 加载更多视频数据（分页加载）
     */
    fun loadMoreVideos() {
        if (_isLoading.value == true || isLastPage) {
            Log.d(
                "MainViewModel", "跳过加载: isLoading=${_isLoading.value}, isLastPage=$isLastPage"
            )
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            videoRepository.getVideos(currentPage, pageSize).fold(onSuccess = { videos ->
                if (videos.isEmpty()) {
                    isLastPage = true
                    Log.d("MainViewModel", "已加载所有视频")
                } else {
                    val currentList = _videoList.value.orEmpty()
                    _videoList.value = currentList + videos
                    isLastPage = videos.size < pageSize
                    currentPage++
                    Log.d(
                        "MainViewModel",
                        "加载更多成功: ${videos.size} 条视频, 总计: ${_videoList.value?.size}"
                    )
                }
            }, onFailure = { error ->
                Log.e("MainViewModel", "加载更多失败", error)
            })
            _isLoading.value = false
        }
    }

    /**
     * 更新当前播放位置
     */
    fun updateCurrentPosition(position: Int) {
        _currentPosition.value = position
        Log.d("ExoPlayerDebug", "Current position updated: $position")
    }

    /**
     * 准备播放指定位置的视频
     */
    fun prepareVideo(position: Int): MediaItem? {
        val videoItem = _videoList.value?.getOrNull(position) ?: return null

        // 移除旧的性能追踪器
        performanceTracker?.let {
            player?.removeListener(it)
        }

        // 创建新的性能追踪器
        performanceTracker = VideoPerformanceTracker(
            videoId = videoItem.id,
            videoPosition = position
        )

        // 添加到播放器
        player?.addListener(performanceTracker!!)

        return MediaItem.fromUri(videoItem.videoUrl)
    }

    /**
     * 播放视频
     */
    fun playVideo(mediaItem: MediaItem) {
        player?.let {
            it.stop()
            it.clearMediaItems()
            it.setMediaItem(mediaItem)
            it.prepare()
            it.playWhenReady = true
        }
    }

    /**
     * 暂停播放
     */
    fun pausePlayback() {
        player?.playWhenReady = false
    }

    /**
     * 恢复播放
     */
    fun resumePlayback() {
        player?.playWhenReady = true
    }

    /**
     * 切换播放/暂停状态
     */
    fun togglePlayback() {
        player?.let {
            it.playWhenReady = !it.playWhenReady
            Log.d("ExoPlayerDebug", "togglePlayback: ${it.playWhenReady}")
        }
    }

    /**
     * 获取当前视频的首帧时间
     * @return 首帧时间（毫秒），如果尚未加载完成则返回 0
     */
    fun getCurrentFirstFrameTime(): Long {
        return performanceTracker?.getFirstFrameTime() ?: 0
    }

    /**
     * 释放播放器资源
     */
    override fun onCleared() {
        super.onCleared()
        // 移除性能追踪器
        performanceTracker?.let {
            player?.removeListener(it)
        }
        performanceTracker = null
        player?.release()
        player = null
    }
}
