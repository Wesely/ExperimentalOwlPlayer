# Pexels API Video Download Guide

## Authentication
To use the Pexels API, you need to include your API key in the Authorization header:
```
Authorization: YOUR_API_KEY
```

## Video Search Endpoint
The main endpoint for searching videos is:
```
GET https://api.pexels.com/videos/search
```

### Input Parameters
- `query` (required): Search term (e.g., "nature", "ocean", "people")
- `per_page` (optional): Number of results per page (default: 15, max: 80)
- `page` (optional): Page number (default: 1)
- `orientation` (optional): Video orientation ("landscape", "portrait", "square")
- `size` (optional): Minimum video size ("large" for 4K, "medium" for Full HD, "small" for HD)

### Example Curl Command
```bash
curl -H "Authorization: 9E2VQntpuCh2Xr4D3CSbNKmsf2x2EMnnyR9wKlZvIRWFNJhpq36QmOgI" \
  "https://api.pexels.com/videos/search?query=nature&per_page=1"
```

### Response Format
The API returns a JSON response with the following structure:
```json
{
  "page": 1,
  "per_page": 1,
  "total_results": 1000,
  "videos": [
    {
      "id": 1234567,
      "width": 1920,
      "height": 1080,
      "url": "https://www.pexels.com/video/1234567/",
      "image": "https://images.pexels.com/videos/1234567/preview.jpg",
      "duration": 30,
      "user": {
        "id": 123,
        "name": "John Doe",
        "url": "https://www.pexels.com/@johndoe"
      },
      "video_files": [
        {
          "id": 123,
          "quality": "hd",
          "file_type": "video/mp4",
          "width": 1920,
          "height": 1080,
          "fps": 30,
          "link": "https://player.vimeo.com/external/1234567.hd.mp4"
        }
      ],
      "video_pictures": [
        {
          "id": 123,
          "picture": "https://static-videos.pexels.com/videos/1234567/pictures/preview-0.jpg",
          "nr": 0
        }
      ]
    }
  ]
}
```

## Downloading Videos
To download a video:
1. Search for videos using the search endpoint
2. From the response, extract the `link` from the `video_files` array
3. Use the link to download the video file

### Example Download Command
```bash
curl -o video.mp4 "https://player.vimeo.com/external/1234567.hd.mp4"
```

## Rate Limits
- Default rate limit: 200 requests per hour
- Monthly quota: 20,000 requests
- Response headers include:
  - X-Ratelimit-Limit: Total monthly requests
  - X-Ratelimit-Remaining: Remaining requests
  - X-Ratelimit-Reset: UNIX timestamp for quota reset

## Attribution Requirements
When using Pexels content, you must:
1. Show a prominent link to Pexels
2. Credit the photographer when possible
3. Include attribution in the format: "Video by [Photographer Name] on Pexels"

## Error Handling
Common HTTP status codes:
- 200: Success
- 400: Bad request
- 401: Unauthorized (invalid API key)
- 429: Too many requests (rate limit exceeded)
