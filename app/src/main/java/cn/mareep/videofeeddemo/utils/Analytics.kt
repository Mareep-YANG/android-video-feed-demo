package cn.mareep.videofeeddemo.utils

import android.util.Log
import org.json.JSONObject

/**
 * 埋点分析工具类
 * 负责收集和上报用户行为和性能数据
 */
object Analytics {

    private const val TAG = "Analytics"
    private var isEnabled = true

    /**
     * 事件类型
     */
    object EventType {
        // 视频播放性能相关
        const val VIDEO_BUFFERING_START = "video_buffering_start"
        const val VIDEO_BUFFERING_END = "video_buffering_end"
        const val VIDEO_READY = "video_ready"
        const val VIDEO_PLAY_START = "video_play_start"
        const val VIDEO_PLAY_ERROR = "video_play_error"
        const val VIDEO_FIRST_FRAME = "video_first_frame"

        // 视频观看行为相关
        const val VIDEO_VIEW_START = "video_view_start"
        const val VIDEO_VIEW_END = "video_view_end"
        const val VIDEO_WATCH_COMPLETE = "video_watch_complete"
        const val VIDEO_PAUSE = "video_pause"
        const val VIDEO_RESUME = "video_resume"
        const val VIDEO_SEEK = "video_seek"

        // 用户交互相关
        const val USER_LIKE = "user_like"
        const val USER_UNLIKE = "user_unlike"
        const val USER_COMMENT = "user_comment"
        const val USER_SHARE = "user_share"
        const val USER_FAVORITE = "user_favorite"
        const val USER_FOLLOW = "user_follow"
        const val USER_AUTHOR_CLICK = "user_author_click"

        // 页面相关
        const val PAGE_LOAD = "page_load"
        const val PAGE_LOAD_MORE = "page_load_more"
    }

    /**
     * 参数键名
     */
    object ParamKey {
        const val VIDEO_ID = "video_id"
        const val VIDEO_POSITION = "position"
        const val DURATION = "duration"
        const val TIMESTAMP = "timestamp"
        const val ERROR_CODE = "error_code"
        const val ERROR_MESSAGE = "error_message"
        const val BUFFERING_COUNT = "buffering_count"
        const val SEEK_FROM = "seek_from"
        const val SEEK_TO = "seek_to"
        const val PAGE_NUMBER = "page_number"
        const val VIDEO_URL = "video_url"
        const val AUTHOR_NAME = "author_name"
    }

    /**
     * 启用或禁用埋点
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * 上报事件（不带参数）
     */
    fun track(eventName: String) {
        track(eventName, emptyMap())
    }

    /**
     * 上报事件（带参数）
     */
    fun track(eventName: String, params: Map<String, Any>) {
        if (!isEnabled) return

        val timestamp = System.currentTimeMillis()
        val eventData = buildEventData(eventName, params, timestamp)

        // 输出到日志（实际项目中这里应该上报到服务器）
        logEvent(eventName, eventData)

        // TODO: 实际项目中，这里应该调用第三方 SDK 或自己的上报接口
    }

    /**
     * 构建事件数据
     */
    private fun buildEventData(
        eventName: String,
        params: Map<String, Any>,
        timestamp: Long
    ): JSONObject {
        return JSONObject().apply {
            put("event_name", eventName)
            put("timestamp", timestamp)
            put("params", JSONObject(params))
        }
    }

    /**
     * 记录日志
     */
    private fun logEvent(eventName: String, eventData: JSONObject) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Event: $eventName")
        Log.d(TAG, "Data: ${eventData.toString(2)}")
        Log.d(TAG, "========================================")
    }

    /**
     * 性能指标统计辅助类
     */
    class PerformanceMetrics {
        private var firstFrameTime: Long = 0
        private var bufferingCount = 0
        private var totalBufferingTime: Long = 0
        private var lastBufferingStartTime: Long = 0

        fun recordFirstFrame() {
            if (firstFrameTime == 0L) {
                firstFrameTime = System.currentTimeMillis()
            }
        }

        fun startBuffering() {
            lastBufferingStartTime = System.currentTimeMillis()
            bufferingCount++
        }

        fun endBuffering() {
            if (lastBufferingStartTime > 0) {
                totalBufferingTime += System.currentTimeMillis() - lastBufferingStartTime
                lastBufferingStartTime = 0
            }
        }

        fun getMetrics(): Map<String, Any> {
            return mapOf(
                "first_frame_time" to firstFrameTime,
                "buffering_count" to bufferingCount,
                "total_buffering_time" to totalBufferingTime
            )
        }

        fun reset() {
            firstFrameTime = 0
            bufferingCount = 0
            totalBufferingTime = 0
            lastBufferingStartTime = 0
        }
    }
}
