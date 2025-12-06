package cn.mareep.videofeeddemo.data.repository

import cn.mareep.videofeeddemo.data.local.entity.VideoItemEntity
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * 视频数据仓库实现类
 * 使用 Mock 数据
 */
class MockVideoRepository @Inject constructor() : VideoRepository {

    // 生成 Mock 数据用于测试分页
    private val mockVideos: List<VideoItemEntity> by lazy {
        generateMockVideos()
    }

    /**
     * 获取分页视频流数据
     */
    override suspend fun getVideos(page: Int, pageSize: Int): Result<List<VideoItemEntity>> {
        return try {
            // 模拟网络延迟
            delay(500)
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, mockVideos.size)

            if (startIndex >= mockVideos.size) {
                // 已经没有更多数据
                Result.success(emptyList())
            } else {
                // 分页返回
                val videos = mockVideos.subList(startIndex, endIndex)
                Result.success(videos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 生成 Mock 视频数据
     */
    private fun generateMockVideos(): List<VideoItemEntity> {
        val videoUrls = listOf(
            "https://vjs.zencdn.net/v/oceans.mp4",
            "https://media.w3.org/2010/05/sintel/trailer.mp4",
            "https://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4"
        )

        val authors = listOf(
            "@Mareep",
            "@Ampharos",
            "@Misoponia",
            "@Elecleus",
            "@Abler",
            "@Kimika",
            "@Sratle",
            "@Lilas"
        )

        val videoNames = listOf(
            "字节跳动工程训练营",
            "工程训练营字节跳动",
            "字节工程训练营跳动",
            "训练营字节工程跳动",
            "训练营跳动字节工程",
            "跳动字节训练营工程",
            "跳动训练营工程字节",
            "跳动工程字节训练营",
            "工程字节训练营跳动",
            "工程训练营跳动字节"
        )

        // 生成 30 条 Mock 数据用于测试分页
        return (1..30).map { index ->
            val videoIndex = (index - 1) % videoUrls.size
            val authorIndex = (index - 1) % authors.size
            val nameIndex = (index - 1) % videoNames.size

            VideoItemEntity(
                id = index.toString(),
                authorName = authors[authorIndex],
                description = videoNames[nameIndex],
                likeCount = generateRandomCount(1000, 100000),
                commentCount = generateRandomCount(100, 10000),
                favoriteCount = generateRandomCount(500, 50000),
                videoUrl = videoUrls[videoIndex]
            )
        }
    }

    /**
     * 生成随机数量字符串
     */
    private fun generateRandomCount(min: Int, max: Int): Int {
        return (min..max).random()
    }
}
