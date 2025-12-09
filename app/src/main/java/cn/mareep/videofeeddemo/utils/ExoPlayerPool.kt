package cn.mareep.videofeeddemo.utils

import android.app.Application
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs

/**
 * ExoPlayer 连接池
 * 管理多个 ExoPlayer 实例,实现视频预加载功能
 *
 * @param context Application 上下文
 * @param poolSize 池大小
 */
class ExoPlayerPool(
    private val context: Application,
    private val poolSize: Int = 3
) {
    companion object {
        private const val TAG = "ExoPlayerPool"
    }

    /**
     * 播放器项数据类
     * @param player ExoPlayer 实例
     * @param boundPosition 绑定的视频位置 (-1 表示未绑定)
     * @param isPreloading 是否正在预加载
     * @param currentListener 当前绑定的监听器（用于清理）
     */
    private data class PlayerItem(
        val player: ExoPlayer,
        var boundPosition: Int = -1,
        var isPreloading: Boolean = false,
        var currentListener: Player.Listener? = null
    )

    // 播放器池
    private val playerItems = mutableListOf<PlayerItem>()

    // 当前正在播放的位置
    private var currentPosition = -1

    // 当前正在播放的播放器
    private var currentPlayer: ExoPlayer? = null

    init {
        // 初始化播放器池
        repeat(poolSize) {
            playerItems.add(PlayerItem(createPlayer()))
        }
        Log.d(TAG, "ExoPlayerPool 初始化完成,池大小: $poolSize")
    }

    /**
     * 创建新的播放器实例
     */
    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateString = when (playbackState) {
                        ExoPlayer.STATE_IDLE -> "STATE_IDLE"
                        ExoPlayer.STATE_BUFFERING -> "STATE_BUFFERING"
                        ExoPlayer.STATE_READY -> "STATE_READY"
                        ExoPlayer.STATE_ENDED -> "STATE_ENDED"
                        else -> "UNKNOWN_STATE"
                    }
                    Log.d(TAG, "onPlaybackStateChanged: $stateString")
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    Log.d(TAG, "onPlayWhenReadyChanged: $playWhenReady, reason: $reason")
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "onPlayerError: ", error)
                }
            })
        }
    }

    /**
     * 获取指定位置的播放器
     * @param position 视频位置
     * @return ExoPlayer 实例,如果未找到则返回 null
     */
    fun getPlayerForPosition(position: Int): ExoPlayer? {
        return playerItems.find { it.boundPosition == position }?.player
    }

    /**
     * 获取当前正在播放的播放器
     */
    fun getCurrentPlayer(): ExoPlayer? {
        return currentPlayer
    }

    /**
     * 准备并播放视频
     * @param position 视频位置
     * @param mediaItem 媒体项
     * @param listener 可选的播放器监听器
     */
    fun prepareAndPlay(position: Int, mediaItem: MediaItem, listener: Player.Listener? = null) {
        Log.d(TAG, "prepareAndPlay: position=$position")

        // 查找或分配播放器
        val playerItem = findOrAllocatePlayer(position)

        // 移除旧的监听器，添加新的监听器
        if (listener != null) {
            // 移除旧的监听器
            playerItem.currentListener?.let {
                playerItem.player.removeListener(it)
                Log.d(TAG, "移除旧监听器: position=$position")
            }
            // 添加新的监听器
            playerItem.player.addListener(listener)
            playerItem.currentListener = listener
            Log.d(TAG, "添加新监听器: position=$position, 当前播放器状态: ${playerItem.player.playbackState}, playWhenReady: ${playerItem.player.playWhenReady}")
        }

        // 如果该位置已经有播放器且已准备好,直接播放
        if (playerItem.boundPosition == position && playerItem.player.playbackState != Player.STATE_IDLE) {
            Log.d(TAG, "播放器已预加载完成,直接播放: position=$position")
            // 暂停其他播放器
            pauseOthers(playerItem.player)

            val wasPlaying = playerItem.player.playWhenReady
            playerItem.player.playWhenReady = true

            // 如果播放器之前不是播放状态，现在开始播放，需要手动触发 onPlayWhenReadyChanged
            if (!wasPlaying && listener != null) {
                Log.d(TAG, "手动触发 onPlayWhenReadyChanged(true)")
                listener.onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            }

            // 如果播放器已经是 READY 状态，手动触发状态回调
            if (playerItem.player.playbackState == Player.STATE_READY && listener != null) {
                Log.d(TAG, "手动触发 onPlaybackStateChanged(READY)")
                listener.onPlaybackStateChanged(Player.STATE_READY)
            }

            playerItem.isPreloading = false
            currentPosition = position
            currentPlayer = playerItem.player
            return
        }

        // 暂停其他播放器
        pauseOthers(playerItem.player)

        // 准备播放
        playerItem.player.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }

        // 更新状态
        playerItem.boundPosition = position
        playerItem.isPreloading = false
        currentPosition = position
        currentPlayer = playerItem.player

        Log.d(TAG, "开始播放: position=$position, player=${playerItem.player.hashCode()}")
    }

    /**
     * 预加载视频(不播放)
     * @param position 视频位置
     * @param mediaItem 媒体项
     */
    fun preloadVideo(position: Int, mediaItem: MediaItem) {
        // 如果该位置已经有播放器,跳过
        if (playerItems.any { it.boundPosition == position }) {
            Log.d(TAG, "位置 $position 已有播放器,跳过预加载")
            return
        }

        Log.d(TAG, "preloadVideo: position=$position")

        // 查找或分配播放器
        val playerItem = findOrAllocatePlayer(position)

        // 准备但不播放
        playerItem.player.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }

        // 更新状态
        playerItem.boundPosition = position
        playerItem.isPreloading = true

        Log.d(TAG, "预加载完成: position=$position, player=${playerItem.player.hashCode()}")
    }

    /**
     * 暂停其他播放器
     */
    private fun pauseOthers(exceptPlayer: ExoPlayer) {
        playerItems.forEach { item ->
            if (item.player != exceptPlayer && item.player.playWhenReady) {
                item.player.playWhenReady = false
                Log.d(TAG, "暂停播放器: position=${item.boundPosition}")
            }
        }
    }

    /**
     * 暂停所有播放
     */
    fun pauseAll() {
        playerItems.forEach { it.player.playWhenReady = false }
        Log.d(TAG, "暂停所有播放器")
    }

    /**
     * 恢复当前播放
     */
    fun resumeCurrent() {
        currentPlayer?.playWhenReady = true
        Log.d(TAG, "恢复当前播放: position=$currentPosition")
    }

    /**
     * 切换播放/暂停状态
     */
    fun togglePlayback() {
        currentPlayer?.let {
            it.playWhenReady = !it.playWhenReady
            Log.d(TAG, "togglePlayback: ${it.playWhenReady}, position=$currentPosition")
        }
    }

    /**
     * 查找或分配播放器
     * @param position 视频位置
     * @return PlayerItem
     */
    private fun findOrAllocatePlayer(position: Int): PlayerItem {
        // 1. 查找已绑定该位置的播放器
        playerItems.find { it.boundPosition == position }?.let {
            Log.d(TAG, "找到已绑定播放器: position=$position")
            return it
        }

        // 2. 查找未绑定的播放器
        playerItems.find { it.boundPosition == -1 }?.let {
            Log.d(TAG, "找到未绑定播放器: position=$position")
            return it
        }

        // 3. 回收距离当前位置最远的播放器
        val recycled = findPlayerToRecycle(position)
        Log.d(TAG, "回收播放器: 从 position=${recycled.boundPosition} 到 position=$position")

        // 清理旧的监听器
        recycled.currentListener?.let {
            recycled.player.removeListener(it)
            recycled.currentListener = null
            Log.d(TAG, "清理被回收播放器的监听器: position=${recycled.boundPosition}")
        }

        return recycled
    }

    /**
     * 查找最适合回收的播放器
     * 优先级:
     * 1. 不是当前播放的
     * 2. 不是预加载的
     * 3. 距离目标位置最远的
     *
     * @param targetPosition 目标位置
     * @return 最适合回收的 PlayerItem
     */
    private fun findPlayerToRecycle(targetPosition: Int): PlayerItem {
        return playerItems
            .filter { it.boundPosition != currentPosition } // 不回收当前播放的
            .maxByOrNull { item ->
                // 计算优先级分数(越大越适合回收)
                var score = abs(item.boundPosition - targetPosition) * 100
                // 如果是预加载的,降低优先级
                if (item.isPreloading) score -= 50
                score
            } ?: playerItems.first() // 如果都不符合,返回第一个
    }

    /**
     * 移除播放器监听器
     * @param position 视频位置
     * @param listener 监听器
     */
    fun removeListener(position: Int, listener: Player.Listener) {
        getPlayerForPosition(position)?.removeListener(listener)
    }

    /**
     * 释放所有资源
     */
    fun releaseAll() {
        playerItems.forEach { it.player.release() }
        playerItems.clear()
        currentPlayer = null
        currentPosition = -1
        Log.d(TAG, "释放所有播放器资源")
    }

    /**
     * 获取池状态信息(用于调试)
     */
    fun getPoolStatus(): String {
        return buildString {
            appendLine("ExoPlayerPool 状态:")
            appendLine("当前播放位置: $currentPosition")
            playerItems.forEachIndexed { index, item ->
                appendLine(
                    "Player $index: position=${item.boundPosition}, " +
                            "preloading=${item.isPreloading}, " +
                            "playing=${item.player.playWhenReady}, " +
                            "state=${item.player.playbackState}"
                )
            }
        }
    }
}
