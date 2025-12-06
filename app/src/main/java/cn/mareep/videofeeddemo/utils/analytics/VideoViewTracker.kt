package cn.mareep.videofeeddemo.utils.analytics

import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity
import cn.mareep.videofeeddemo.utils.Analytics

/**
 * 视频观看行为追踪器
 * 监控用户的视频观看行为，包括观看时长、滑动切换等
 */
class VideoViewTracker(
    private val getVideoAtPosition: (Int) -> VideoItemEntity?
) : ViewPager2.OnPageChangeCallback() {

    companion object {
        private const val TAG = "VideoViewTracker"
        private const val MIN_VALID_WATCH_TIME = 500L // 最小有效观看时长（毫秒）
    }

    // 当前视频信息
    private var currentPosition = -1
    private var currentVideoId = ""
    private var viewStartTime: Long = 0
    private var totalWatchTime: Long = 0

    // 用于计算完播率
    private var videoDuration: Long = 0

    /**
     * 页面被选中
     */
    override fun onPageSelected(position: Int) {
        super.onPageSelected(position)

        // 上报上一个视频的观看完成事件
        if (currentPosition >= 0 && viewStartTime > 0) {
            reportVideoWatchComplete()
        }

        // 开始追踪新视频
        startTrackingVideo(position)
    }

    /**
     * 滚动状态改变
     */
    override fun onPageScrollStateChanged(state: Int) {
        super.onPageScrollStateChanged(state)

        when (state) {
            ViewPager2.SCROLL_STATE_DRAGGING -> {
                // 用户开始拖动，暂停计时
                pauseTracking()
            }

            ViewPager2.SCROLL_STATE_SETTLING -> {
                // 正在自动滚动到目标位置
                Log.d(TAG, "页面滚动中...")
            }

            ViewPager2.SCROLL_STATE_IDLE -> {
                // 滚动停止，恢复计时
                resumeTracking()
            }
        }
    }

    /**
     * 开始追踪视频
     */
    private fun startTrackingVideo(position: Int) {
        val video = getVideoAtPosition(position) ?: return

        currentPosition = position
        currentVideoId = video.id
        viewStartTime = System.currentTimeMillis()
        totalWatchTime = 0

        // 上报视频浏览开始事件
        Analytics.track(
            Analytics.EventType.VIDEO_VIEW_START,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to video.id,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.VIDEO_URL to video.videoUrl,
                Analytics.ParamKey.AUTHOR_NAME to video.authorName,
                Analytics.ParamKey.TIMESTAMP to viewStartTime
            )
        )

        Log.d(TAG, "开始追踪视频: position=$position, videoId=${video.id}")
    }

    /**
     * 暂停追踪（用户开始滑动时）
     */
    private fun pauseTracking() {
        if (viewStartTime > 0) {
            val currentWatchTime = System.currentTimeMillis() - viewStartTime
            totalWatchTime += currentWatchTime
            viewStartTime = 0 // 重置开始时间
            Log.d(TAG, "暂停追踪，当前观看时长: ${currentWatchTime}ms")
        }
    }

    /**
     * 恢复追踪（滚动停止时）
     */
    private fun resumeTracking() {
        if (viewStartTime == 0L && currentPosition >= 0) {
            viewStartTime = System.currentTimeMillis()
            Log.d(TAG, "恢复追踪")
        }
    }

    /**
     * 上报视频观看完成
     */
    private fun reportVideoWatchComplete() {
        // 计算总观看时长
        val finalWatchTime = if (viewStartTime > 0) {
            totalWatchTime + (System.currentTimeMillis() - viewStartTime)
        } else {
            totalWatchTime
        }

        // 只上报有效的观看（观看时长超过阈值）
        if (finalWatchTime < MIN_VALID_WATCH_TIME) {
            Log.d(TAG, "观看时长过短，不上报: ${finalWatchTime}ms")
            return
        }

        val video = getVideoAtPosition(currentPosition)

        Analytics.track(
            Analytics.EventType.VIDEO_WATCH_COMPLETE,
            mutableMapOf<String, Any>(
                Analytics.ParamKey.VIDEO_ID to currentVideoId,
                Analytics.ParamKey.VIDEO_POSITION to currentPosition,
                Analytics.ParamKey.DURATION to finalWatchTime
            ).apply {
                if (video != null) {
                    put(Analytics.ParamKey.AUTHOR_NAME, video.authorName)
                    put("video_desc", video.description)
                }
            }
        )

        Log.d(
            TAG,
            "视频观看完成: position=$currentPosition, videoId=$currentVideoId, duration=${finalWatchTime}ms"
        )
    }

    /**
     * 手动上报当前视频观看结束（例如在页面退出时调用）
     */
    fun reportCurrentVideoEnd() {
        if (currentPosition >= 0) {
            reportVideoWatchComplete()
        }
    }

    /**
     * 重置追踪器
     */
    fun reset() {
        currentPosition = -1
        currentVideoId = ""
        viewStartTime = 0
        totalWatchTime = 0
        videoDuration = 0
    }
}
