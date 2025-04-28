# OwlPlayer

## Overview
OwlPlayer is a feature-rich video player application that allows users to search, stream, and download videos from Pexels. It provides robust offline functionality, high-quality playback options, and an intuitive user interface.

## Architecture
The application follows modern Android development best practices:
- MVVM architecture with Clean Architecture principles
- Repository pattern for data operations
- Retrofit + OkHttp for network communication
- Dependency injection using Hilt

## API Endpoints
The application integrates with two main Pexels API endpoints:

1. **Video Search**: `https://api.pexels.com/videos/search`
   - Used for searching videos with parameters like query, per_page, and page
   - Returns video collections with metadata and available formats

2. **Video by ID**: `https://api.pexels.com/videos/videos/{id}`
   - Used to fetch a specific video by its unique identifier
   - Returns detailed information about a single video

# Features

## Video Discovery
- Search functionality for Pexels video library
- Video fetching by ID
- Multiple quality options (HD, Full HD, 4K)

## Offline Capabilities
- Download videos for offline viewing using OkHttp for file downloads
- Persistent storage through SharedPreferences for downloaded video metadata
- StateFlow-based download progress tracking in real-time (0-100%)
- Full download management with pause, resume, and cancel functionality
- Automatic cleanup of partial downloads on error

## Connectivity Management
- Automatic network state monitoring
- Seamless handling of connectivity changes
- Initial content loading when network becomes available

# Further Enhancement
- If network status is critical, consider exponential fallback recovery schema.
- Determine if we're gonna share the video contents that we've downloaded with System's Gallery
- Implement Room database to create structured relationships between downloaded video files and Pexels metadata
- Add a storage management interface for users to view and manage downloaded content
- Define a policy for handling videos that are no longer available in Pexels API (display or remove local data?)