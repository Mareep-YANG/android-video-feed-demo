package cn.mareep.videofeeddemo.data.local.entity

data class VideoItemEntity(
    val id: String,
    val authorName: String,
    val description: String,
    val likeCount: String,
    val commentCount: String,
    val favoriteCount: String,
    val videoUrl: String
)