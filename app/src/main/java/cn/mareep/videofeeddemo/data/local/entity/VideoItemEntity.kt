package cn.mareep.videofeeddemo.data.local.entity

data class VideoItemEntity(
    val id: String,
    val authorName: String,
    val description: String,
    val likeCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val videoUrl: String
)