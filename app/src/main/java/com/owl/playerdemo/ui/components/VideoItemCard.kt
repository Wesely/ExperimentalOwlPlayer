package com.owl.playerdemo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.owl.playerdemo.R
import com.owl.playerdemo.model.VideoItem
import com.owl.playerdemo.ui.viewmodel.MainViewModel

@Composable
fun VideoItemCard(
    video: VideoItem,
    onDownloadClick: (VideoItem) -> Unit = {},
    onPlayClick: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    // Check if this video is already downloaded
    val isDownloaded = viewModel.isVideoDownloaded(video.id)
    
    // Get download progress for this video
    val downloads by viewModel.downloadsInProgress.collectAsState()
    val downloadProgress = downloads[video.id] ?: 0f
    val isDownloading = downloadProgress > 0f && downloadProgress < 100f
    
    Column(
        modifier = modifier
            .width(320.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = video.imageUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Duration indicator in bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${video.duration}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Download/Play button or Progress Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDownloading -> {
                        // Show progress indicator with percentage
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            
                            // Show percentage in center
                            Text(
                                text = "${downloadProgress.toInt()}%",
                                color = Color.White,
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    isDownloaded -> {
                        // Play button
                        IconButton(
                            onClick = { onPlayClick(video) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play),
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    else -> {
                        // Download button
                        IconButton(
                            onClick = { onDownloadClick(video) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Video Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "By ${video.user.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quality: ${video.videoFiles.firstOrNull()?.quality?.uppercase() ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${video.videoFiles.firstOrNull()?.width ?: 0}×${video.videoFiles.firstOrNull()?.height ?: 0}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Add file size
            Text(
                text = "Size: ${video.videoFiles.firstOrNull()?.getFormattedSize() ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Show download status
            when {
                isDownloading -> {
                    Text(
                        text = "Downloading ${downloadProgress.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Blue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                isDownloaded -> {
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
} 