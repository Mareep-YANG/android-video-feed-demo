package cn.mareep.videofeeddemo.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.mareep.videofeeddemo.databinding.ActivityMainBinding
import cn.mareep.videofeeddemo.ui.main.adapter.VideoFeedAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: VideoFeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 配置Edge to Edge
        enableEdgeToEdge()
        // 绑定View
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 播放相关初始化
        viewModel.initializePlayer()
        observeViewModel()
        setupViewPagerListener()
        // 绑定
    }

    /**
     * 观察ViewModel
     */
    private fun observeViewModel() {
        // 观察视频列表数据
        viewModel.videoList.observe(this) { videoList ->
            // 如果是首次初始化
            if (::adapter.isInitialized.not()) {
                adapter = VideoFeedAdapter(videoList)
                binding.viewPager.adapter = adapter

                // 初始播放第一个视频
                binding.viewPager.post {
                    playVideoAtPosition(0)
                }
            }
        }
    }

    /**
     * 初始化监听上下拉Page
     */
    private fun setupViewPagerListener() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.updateCurrentPosition(position)
                playVideoAtPosition(position)
            }
        })
    }

    /**
     * 在指定位置播放视频
     */
    private fun playVideoAtPosition(position: Int) {
        val mediaItem = viewModel.prepareVideo(position) ?: return
        val viewHolder = getViewHolderAtPosition(position) ?: return
        // 将播放器绑定到 PlayerView
        viewHolder.binding.videoView.player = viewModel.getPlayer()
        // 播放视频
        viewModel.playVideo(mediaItem)
    }

    /**
     * 获取指定位置上的Holder
     */
    private fun getViewHolderAtPosition(position: Int): VideoFeedAdapter.VideoViewHolder? {
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(position) as? VideoFeedAdapter.VideoViewHolder
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
        // ViewModel 会自动释放播放器资源
    }
}