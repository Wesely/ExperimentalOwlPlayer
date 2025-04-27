Pexels API key: 9E2VQntpuCh2Xr4D3CSbNKmsf2x2EMnnyR9wKlZvIRWFNJhpq36QmOgI

Pexels API Doc:
https://www.pexels.com/api/documentation/

## Features Added

### Pexels API Integration
- Added video search functionality from Pexels
- Support for fetching individual videos by ID
- Video quality options (HD, Full HD, 4K)
- Includes video metadata (duration, dimensions, fps)

### Video Download & Offline Playback
- Download videos for offline viewing
- Persistent storage of download information between app sessions
- Track downloaded status of videos
- Play local videos without internet connection
- View all downloaded videos as a separate collection

### Technical Implementation
- MVVM architecture with Clean Architecture principles
- Repository pattern for data operations
- Retrofit + OkHttp for network calls
- Dependency injection with Hilt

# Connectivity 
- Network state monitoring via NetworkConnectivityManager
- Automatic detection of connectivity changes through NetworkCallback
- Offline support with fallback sample video content
- Error handling with user-friendly messages and retry option
- Auto-retry when connection is restored