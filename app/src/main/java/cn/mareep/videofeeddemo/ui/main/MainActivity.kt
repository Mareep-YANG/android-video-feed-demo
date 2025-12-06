package cn.mareep.videofeeddemo.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.mareep.videofeeddemo.databinding.ActivityMainBinding
import cn.mareep.videofeeddemo.ui.main.adapter.VideoFeedAdapter
import cn.mareep.videofeeddemo.utils.Analytics
import cn.mareep.videofeeddemo.utils.analytics.VideoViewTracker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), VideoFeedAdapter.VideoInteractionListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: VideoFeedAdapter

    // 视频观看行为追踪器
    private lateinit var videoViewTracker: VideoViewTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 配置Edge to Edge
        enableEdgeToEdge()
        // 绑定View
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 播放相关初始化
        viewModel.initializePlayer()
        // 初始化视频观看追踪器
        initVideoViewTracker()
        observeViewModel()
        setupViewPagerListener()

    }

    /**
     * Activity 失去焦点，但可见
     */
    override fun onPause() {
        super.onPause()
        viewModel.pausePlayback()
    }

    /**
     * Activity进入前台并与用户交互
     */
    override fun onResume() {
        super.onResume()
        viewModel.resumePlayback()
    }

    /**
     * Activity被销毁
     */
    override fun onDestroy() {
        super.onDestroy()
        // 上报当前视频观看结束
        if (::videoViewTracker.isInitialized) {
            videoViewTracker.reportCurrentVideoEnd()
        }
        // ViewModel 会自动释放播放器资源
    }

    /**
     * 观察ViewModel
     */
    private fun observeViewModel() {
        // 观察视频列表数据
        viewModel.videoList.observe(this) { videoList ->
            // 如果是首次初始化，初始化Adapter
            if (::adapter.isInitialized.not()) {
                adapter = VideoFeedAdapter(videoList, this)
                binding.viewPager.adapter = adapter

                // 初始播放第一个视频
                binding.viewPager.post {
                    playVideoAtPosition(0)
                }
            } else {
                // 数据更新时，通知Adapter
                adapter.updateVideos(videoList)
            }
        }
    }

    /**
     * 初始化视频观看追踪器
     */
    private fun initVideoViewTracker() {
        videoViewTracker = VideoViewTracker { position ->
            viewModel.videoList.value?.getOrNull(position)
        }
    }

    /**
     * 初始化监听上下拉Page
     */
    private fun setupViewPagerListener() {
        // 注册视频观看追踪器
        binding.viewPager.registerOnPageChangeCallback(videoViewTracker)

        // 注册原有的页面切换逻辑
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.updateCurrentPosition(position)
                playVideoAtPosition(position)

                // 分页加载逻辑：当滑动到倒数第3个视频时，加载更多
                val totalCount = adapter.itemCount
                if (position >= totalCount - 3) {
                    viewModel.loadMoreVideos()

                    // 上报加载更多事件
                    Analytics.track(
                        Analytics.EventType.PAGE_LOAD_MORE,
                        mapOf(
                            Analytics.ParamKey.PAGE_NUMBER to (position / 10 + 1),
                            "trigger_position" to position
                        )
                    )
                }
            }
        })
    }

    /**
     * 在指定位置播放视频
     */
    private fun playVideoAtPosition(position: Int) {
        val mediaItem = viewModel.prepareVideo(position) ?: return
        val viewHolder = getViewHolderAtPosition(position) ?: return
        // 使用 setPlayer 方法绑定播放器并添加状态监听
        viewHolder.setPlayer(viewModel.getPlayer())
        // 播放视频
        viewModel.playVideo(mediaItem)
        // 开始更新进度
        viewHolder.startProgressUpdate()
    }

    /**
     * 获取指定位置上的Holder
     */
    private fun getViewHolderAtPosition(position: Int): VideoFeedAdapter.VideoViewHolder? {
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(position) as? VideoFeedAdapter.VideoViewHolder
    }

    /**
     * 视频区域点击 - 切换播放/暂停
     */
    override fun onVideoClick() {
        viewModel.togglePlayback()
    }

    /**
     * 点赞按钮点击
     */
    override fun onLikeClick(videoId: String, position: Int) {
        // 上报点赞事件
        Analytics.track(
            Analytics.EventType.USER_LIKE,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to videoId,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.TIMESTAMP to System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "点赞", Toast.LENGTH_SHORT).show()
    }

    /**
     * 评论按钮点击
     */
    override fun onCommentClick(videoId: String, position: Int) {
        // 上报评论事件
        Analytics.track(
            Analytics.EventType.USER_COMMENT,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to videoId,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.TIMESTAMP to System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "评论", Toast.LENGTH_SHORT).show()
    }

    /**
     * 分享按钮点击
     */
    override fun onShareClick(videoId: String, position: Int) {
        // 上报分享事件
        Analytics.track(
            Analytics.EventType.USER_SHARE,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to videoId,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.TIMESTAMP to System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "分享", Toast.LENGTH_SHORT).show()
    }

    /**
     * 作者信息点击
     */
    override fun onAuthorClick(videoId: String, position: Int) {
        // 上报作者信息点击事件
        val video = viewModel.videoList.value?.getOrNull(position)
        Analytics.track(
            Analytics.EventType.USER_AUTHOR_CLICK,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to videoId,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.AUTHOR_NAME to (video?.authorName ?: ""),
                Analytics.ParamKey.TIMESTAMP to System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "作者信息", Toast.LENGTH_SHORT).show()
    }

    /**
     * 收藏按钮点击
     */
    override fun onFavoriteClick(videoId: String, position: Int) {
        // 上报收藏事件
        Analytics.track(
            Analytics.EventType.USER_FAVORITE,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to videoId,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.TIMESTAMP to System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "收藏", Toast.LENGTH_SHORT).show()
    }

    /**
     * 关注按钮点击
     */
    override fun onFollowClick(videoId: String, position: Int) {
        // 上报关注事件
        val video = viewModel.videoList.value?.getOrNull(position)
        Analytics.track(
            Analytics.EventType.USER_FOLLOW,
            mapOf(
                Analytics.ParamKey.VIDEO_ID to videoId,
                Analytics.ParamKey.VIDEO_POSITION to position,
                Analytics.ParamKey.AUTHOR_NAME to (video?.authorName ?: ""),
                Analytics.ParamKey.TIMESTAMP to System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "关注", Toast.LENGTH_SHORT).show()
    }

}