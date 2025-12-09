package cn.mareep.videofeeddemo.ui.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
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

    // 全屏状态跟踪
    private var isFullscreen = false

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
     * 处理配置变更（屏幕旋转）
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // 进入横屏，启用全屏模式
                enterFullscreen()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                // 恢复竖屏，退出全屏模式
                exitFullscreen()
            }
        }

        // 强制刷新布局以应用正确的横屏/竖屏布局
        if (::adapter.isInitialized) {
            val currentPosition = binding.viewPager.currentItem
            val videoList = viewModel.videoList.value ?: return

            // 暂停当前播放
            viewModel.pausePlayback()

            // 获取 RecyclerView 并清空缓存池
            val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
            recyclerView?.recycledViewPool?.clear()

            // 重新创建 Adapter
            adapter = VideoFeedAdapter(videoList, this)
            binding.viewPager.adapter = adapter

            // 恢复到当前位置
            binding.viewPager.setCurrentItem(currentPosition, false)

            // 等待布局完成后重新播放
            binding.viewPager.postDelayed({
                playVideoAtPosition(currentPosition)
                viewModel.resumePlayback()
            }, 150)
        }
    }

    /**
     * 进入全屏模式
     */
    private fun enterFullscreen() {
        isFullscreen = true

        // 隐藏 ActionBar
        supportActionBar?.hide()

        // 隐藏 MainActivity 的 top bar（搜索栏、返回按钮等）
        binding.btnBack.visibility = View.GONE
        binding.tvSearchBar.visibility = View.GONE
        binding.btnMore.visibility = View.GONE

        // 隐藏系统栏
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ 使用新API
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars()
                            or android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 11 以下使用传统方式
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }

        // 禁用 ViewPager2 滑动（横屏时专注观看当前视频）
        binding.viewPager.isUserInputEnabled = false
    }

    /**
     * 退出全屏模式
     */
    private fun exitFullscreen() {
        isFullscreen = false

        // 显示 ActionBar
        supportActionBar?.show()

        // 显示 MainActivity 的 top bar
        binding.btnBack.visibility = View.VISIBLE
        binding.tvSearchBar.visibility = View.VISIBLE
        binding.btnMore.visibility = View.VISIBLE

        // 显示系统栏
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.show(
                android.view.WindowInsets.Type.statusBars()
                        or android.view.WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        // 恢复 ViewPager2 滑动
        binding.viewPager.isUserInputEnabled = true
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

                // 预加载下一个视频
                preloadNextVideo(position)

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

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // 监听滑动状态变化,可用于进一步优化预加载
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        // 用户开始拖动
                    }
                    ViewPager2.SCROLL_STATE_SETTLING -> {
                        // 滑动中
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        // 滑动结束
                    }
                }
            }
        })
    }

    /**
     * 在指定位置播放视频
     */
    private fun playVideoAtPosition(position: Int, retryCount: Int = 0) {
        viewModel.prepareVideo(position)
        val viewHolder = getViewHolderAtPosition(position)

        if (viewHolder == null) {
            // ViewHolder 还没有创建完成，重试最多3次
            if (retryCount < 3) {
                binding.viewPager.postDelayed({
                    playVideoAtPosition(position, retryCount + 1)
                }, 50)
            }
            return
        }

        // 使用 setPlayer 方法绑定播放器并添加状态监听
        viewHolder.setPlayer(viewModel.getCurrentPlayer())
        // 开始更新进度
        viewHolder.startProgressUpdate()
    }

    /**
     * 预加载下一个视频
     */
    private fun preloadNextVideo(currentPosition: Int) {
        val nextPosition = currentPosition + 1
        if (nextPosition < adapter.itemCount) {
            viewModel.preloadVideo(nextPosition)
        }
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