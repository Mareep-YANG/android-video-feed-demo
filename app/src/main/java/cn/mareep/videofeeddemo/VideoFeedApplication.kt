package cn.mareep.videofeeddemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用程序主入口
 * 使用 Hilt 进行依赖注入
 */
@HiltAndroidApp
class VideoFeedApplication : Application()
