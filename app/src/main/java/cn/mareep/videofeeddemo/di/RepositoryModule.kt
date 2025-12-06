package cn.mareep.videofeeddemo.di

import cn.mareep.videofeeddemo.data.repository.VideoRepository
import cn.mareep.videofeeddemo.data.repository.MockVideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 提供 VideoRepository 实现
     */
    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        videoRepositoryImpl: MockVideoRepository
    ): VideoRepository
}
