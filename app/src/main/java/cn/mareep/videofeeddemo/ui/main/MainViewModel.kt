package cn.mareep.videofeeddemo.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _videoList = MutableLiveData<List<VideoItemEntity>>()
    val videoList: LiveData<List<VideoItemEntity>> = _videoList

    private val _currentPosition = MutableLiveData(0)

    private var player: ExoPlayer? = null

    init {
        loadVideoData()
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
     * 加载视频数据
     */
    private fun loadVideoData() {
        // TODO: 将来从 Repository 获取数据
        val dummyData = listOf(
            VideoItemEntity(
                "1",
                "@Mareep",
                "字节跳动工程训练营",
                "1422",
                "306",
                "319",
                "https://vjs.zencdn.net/v/oceans.mp4"
            ),
            VideoItemEntity(
                "2",
                "@TechGuru",
                "The future of AI is here! Check out this amazing demo. #AI #Tech",
                "8.5w",
                "1.2k",
                "5k",
                "https://media.w3.org/2010/05/sintel/trailer.mp4"
            ),
            VideoItemEntity(
                "3",
                "@NatureLover",
                "Beautiful sunset in the mountains. 🏔️☀️ #Nature #Travel",
                "23k",
                "400",
                "1.1k",
                "https://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4"
            )
        )
        _videoList.value = dummyData
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
     * 释放播放器资源
     */
    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }
}
