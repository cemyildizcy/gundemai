package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.presentation.safePlayableVideoUrl
import com.example.ui.theme.BrandTeal

@Composable
fun ArticleVideoPlayer(
    videoUrl: String,
    posterUrl: String?,
    articleTitle: String,
    category: String,
    modifier: Modifier = Modifier,
) {
    val playableUrl = safePlayableVideoUrl(videoUrl) ?: return
    var playbackStarted by rememberSaveable(playableUrl) { mutableStateOf(false) }
    var playbackFailed by remember(playableUrl) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Haber videosu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Uygulama içinde",
                style = MaterialTheme.typography.labelSmall,
                color = BrandTeal,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (playbackStarted) {
            InlineVideoSurface(
                videoUrl = playableUrl,
                onPlaybackError = {
                    playbackFailed = true
                    playbackStarted = false
                },
                modifier = Modifier.testTag("article_video_player"),
            )
        } else {
            Surface(
                onClick = {
                    playbackFailed = false
                    playbackStarted = true
                },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .testTag("article_video_player"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!posterUrl.isNullOrBlank()) {
                        ArticleImage(
                            imageUrl = posterUrl,
                            title = articleTitle,
                            category = category,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.34f)),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandTeal,
                            modifier = Modifier.size(58.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playbackFailed) Icons.Default.Replay else Icons.Default.PlayArrow,
                                    contentDescription = if (playbackFailed) "Videoyu yeniden dene" else "Videoyu oynat",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                        Text(
                            text = if (playbackFailed) "Video yüklenemedi · tekrar dene" else "Videoyu oynat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (posterUrl.isNullOrBlank()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineVideoSurface(
    videoUrl: String,
    onPlaybackError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player, onPlaybackError) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = true
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp)),
    )
}
