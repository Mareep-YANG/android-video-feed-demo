package cn.mareep.videofeeddemo.utils.analytics

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import cn.mareep.videofeeddemo.utils.Analytics

/**
 * 视频播放性能监控拦截器
 * 自动追踪播放器的性能指标，包括缓冲、错误、加载等
 */
class VideoPerformanceTracker(
    private val videoId: String = "",
    private val videoPosition: Int = -1
) : Player.Listener {

    companion object {
        private const val TAG = "VideoPerformanceTracker"
    }

    // 性能指标记录
    private val metrics = Analytics.PerformanceMetrics()
    private var bufferingStartTime: Long = 0
    private var videoStartTime: Long = System.currentTimeMillis() // 创建时立即记录开始时间
    private var isFirstFrame = true
    private var playStartTime: Long = 0
    private var firstFrameTime: Long = 0 // 记录首帧时间

    /**
     * 播放状态改变
     */
    override fun onPlaybackStateChanged(playbackState: Int) {
        val currentTime = System.currentTimeMillis()

        when (playbackState) {
            Player.STATE_IDLE -> {
                Log.d(TAG, "播放器状态: IDLE")
            }

            Player.STATE_BUFFERING -> {
                // 缓冲开始
                bufferingStartTime = currentTime
                metrics.startBuffering()

                Analytics.track(
                    Analytics.EventType.VIDEO_BUFFERING_START,
                    buildBaseParams().toMutableMap().apply {
                        put(Analytics.ParamKey.TIMESTAMP, currentTime)
                    }
                )
                Log.d(TAG, "缓冲开始")
            }

            Player.STATE_READY -> {
                // 缓冲结束或准备完成
                if (bufferingStartTime > 0) {
                    val bufferingDuration = currentTime - bufferingStartTime
                    metrics.endBuffering()

                    Analytics.track(
                        Analytics.EventType.VIDEO_BUFFERING_END,
                        buildBaseParams().toMutableMap().apply {
                            put(Analytics.ParamKey.DURATION, bufferingDuration)
                            put(Analytics.ParamKey.BUFFERING_COUNT, metrics.getMetrics()["buffering_count"] ?: 0)
                        }
                    )
                    Log.d(TAG, "缓冲结束，耗时: ${bufferingDuration}ms")
                    bufferingStartTime = 0
                }

                // 记录首帧时间
                if (isFirstFrame) {
                    isFirstFrame = false
                    firstFrameTime = currentTime - videoStartTime
                    metrics.recordFirstFrame()

                    Analytics.track(
                        Analytics.EventType.VIDEO_FIRST_FRAME,
                        buildBaseParams().toMutableMap().apply {
                            put("first_frame_time", firstFrameTime)
                        }
                    )
                    Log.i(TAG, "========================================")
                    Log.i(TAG, "FIRST FRAME TIME: ${firstFrameTime}ms")
                    Log.i(TAG, "Video ID: $videoId, Position: $videoPosition")
                    Log.i(TAG, "========================================")
                }

                Analytics.track(
                    Analytics.EventType.VIDEO_READY,
                    buildBaseParams()
                )
            }

            Player.STATE_ENDED -> {
                Log.d(TAG, "播放结束")
                // 可以在这里上报完整的性能指标
                reportFinalMetrics()
            }
        }
    }

    /**
     * 播放/暂停状态改变
     */
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        val currentTime = System.currentTimeMillis()

        if (playWhenReady) {
            // 开始播放
            playStartTime = currentTime

            Analytics.track(
                Analytics.EventType.VIDEO_PLAY_START,
                buildBaseParams().toMutableMap().apply {
                    put(Analytics.ParamKey.TIMESTAMP, currentTime)
                    put("play_reason", reason)
                }
            )
            Log.d(TAG, "播放开始")
        } else {
            // 暂停播放
            if (playStartTime > 0) {
                val playDuration = currentTime - playStartTime
                Analytics.track(
                    Analytics.EventType.VIDEO_PAUSE,
                    buildBaseParams().toMutableMap().apply {
                        put(Analytics.ParamKey.DURATION, playDuration)
                        put("pause_reason", reason)
                    }
                )
                Log.d(TAG, "播放暂停，播放时长: ${playDuration}ms")
            }
        }
    }

    /**
     * 播放错误
     */
    override fun onPlayerError(error: PlaybackException) {
        Analytics.track(
            Analytics.EventType.VIDEO_PLAY_ERROR,
            buildBaseParams().toMutableMap().apply {
                put(Analytics.ParamKey.ERROR_CODE, error.errorCode)
                put(Analytics.ParamKey.ERROR_MESSAGE, error.message ?: "Unknown error")
                put("error_type", error::class.java.simpleName)
            }
        )
        Log.e(TAG, "播放错误: ${error.message}", error)
    }

    /**
     * 位置变化（可用于检测 seek 操作）
     */
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            Analytics.track(
                Analytics.EventType.VIDEO_SEEK,
                buildBaseParams().toMutableMap().apply {
                    put(Analytics.ParamKey.SEEK_FROM, oldPosition.positionMs)
                    put(Analytics.ParamKey.SEEK_TO, newPosition.positionMs)
                }
            )
            Log.d(TAG, "用户拖动进度条: ${oldPosition.positionMs}ms -> ${newPosition.positionMs}ms")
        }
    }

    /**
     * 构建基础参数
     */
    private fun buildBaseParams(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            if (videoId.isNotEmpty()) {
                put(Analytics.ParamKey.VIDEO_ID, videoId)
            }
            if (videoPosition >= 0) {
                put(Analytics.ParamKey.VIDEO_POSITION, videoPosition)
            }
        }
    }

    /**
     * 上报最终性能指标
     */
    private fun reportFinalMetrics() {
        val finalMetrics = metrics.getMetrics()
        Analytics.track(
            "video_performance_summary",
            buildBaseParams().toMutableMap().apply {
                putAll(finalMetrics)
                put("total_play_time", System.currentTimeMillis() - videoStartTime)
            }
        )
        Log.d(TAG, "性能汇总: $finalMetrics")
    }

    /**
     * 获取首帧时间
     * @return 首帧时间（毫秒），如果尚未加载完成则返回 0
     */
    fun getFirstFrameTime(): Long {
        return firstFrameTime
    }
}
