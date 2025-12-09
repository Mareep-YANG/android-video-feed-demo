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
import cn.mareep.videofeeddemo.utils.ExoPlayerPool
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

    private var playerPool: ExoPlayerPool? = null
    private var currentPage = 0
    private val pageSize = 10
    private var isLastPage = false

    // 性能监控追踪器 Map (每个位置对应一个追踪器)
    private val performanceTrackers = mutableMapOf<Int, VideoPerformanceTracker>()

    init {
        loadInitialVideos()
    }

    /**
     * 初始化播放器池
     */
    fun initializePlayer(): ExoPlayerPool {
        if (playerPool == null) {
            playerPool = ExoPlayerPool(getApplication(), poolSize = 3)
            Log.d("MainViewModel", "ExoPlayerPool 初始化完成")
        }
        return playerPool!!
    }

    /**
     * 获取播放器池实例
     */
    fun getPlayerPool(): ExoPlayerPool? = playerPool

    /**
     * 获取指定位置的播放器
     */
    fun getPlayer(position: Int): ExoPlayer? = playerPool?.getPlayerForPosition(position)

    /**
     * 获取当前播放器
     */
    fun getCurrentPlayer(): ExoPlayer? = playerPool?.getCurrentPlayer()

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
        val mediaItem = MediaItem.fromUri(videoItem.videoUrl)

        // 创建性能追踪器
        val tracker = VideoPerformanceTracker(
            videoId = videoItem.id,
            videoPosition = position
        )
        performanceTrackers[position] = tracker

        // 准备并播放
        playerPool?.prepareAndPlay(position, mediaItem, tracker)

        Log.d("MainViewModel", "准备播放视频: position=$position, url=${videoItem.videoUrl}")
        return mediaItem
    }

    /**
     * 预加载指定位置的视频
     */
    fun preloadVideo(position: Int) {
        val videoItem = _videoList.value?.getOrNull(position) ?: return
        val mediaItem = MediaItem.fromUri(videoItem.videoUrl)

        playerPool?.preloadVideo(position, mediaItem)
        Log.d("MainViewModel", "预加载视频: position=$position, url=${videoItem.videoUrl}")
    }

    /**
     * 暂停播放
     */
    fun pausePlayback() {
        playerPool?.pauseAll()
    }

    /**
     * 恢复播放
     */
    fun resumePlayback() {
        playerPool?.resumeCurrent()
    }

    /**
     * 切换播放/暂停状态
     */
    fun togglePlayback() {
        playerPool?.togglePlayback()
    }

    /**
     * 获取播放器池状态（调试用）
     */
    fun getPoolStatus(): String {
        return playerPool?.getPoolStatus() ?: "ExoPlayerPool 未初始化"
    }

    /**
     * 释放播放器资源
     */
    override fun onCleared() {
        super.onCleared()
        // 清理所有性能追踪器
        performanceTrackers.forEach { (position, tracker) ->
            playerPool?.removeListener(position, tracker)
        }
        performanceTrackers.clear()
        // 释放播放器池
        playerPool?.releaseAll()
        playerPool = null
        Log.d("MainViewModel", "释放播放器池资源")
    }
}
