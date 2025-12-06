package cn.mareep.videofeeddemo.data.repository

import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity

/**
 * 视频数据仓库接口
 */
interface VideoRepository {

    /**
     * 分页获取视频列表
     *
     * @param page 页码(从 0 开始)
     * @param pageSize 每页数量
     * @return Result 包装的视频列表
     */
    suspend fun getVideos(page: Int, pageSize: Int): Result<List<VideoItemEntity>>
}
