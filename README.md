# WatchMate Backend

WatchMate is a Spring Boot backend for movie and TV tracking. It supports movie and show status tracking, contiguous show progress, watchlists, favourites, reviews, dashboard views, a social graph, and TMDB-backed catalog/discovery data.

## Overview

- Java 21, Spring Boot 4, Maven
- MySQL + Flyway for persistence
- Redis-backed caching for public metadata and user-specific summaries
- JWT access/refresh token authentication
- TMDB integration with local catalog caching and show metadata hydration
- Background show-tracking jobs for asynchronous show hydration and progress/status completion
- Docker Compose for local app/MySQL/Redis setup
- Testcontainers-based integration tests

## Architecture

The backend follows a layered structure:

- Controllers expose REST endpoints and OpenAPI annotations.
- Services own business logic, validation, TMDB orchestration, caching, and async fallback decisions.
- Repositories provide Spring Data JPA persistence access.
- Entities and DTOs model database state and request/response payloads.

Current backend areas include:

- Auth and account verification
- Discovery and genre browsing
- Movie details and movie watch status
- Show metadata, show tracking status, and canonical show progress
- Watchlists, favourites, and reviews
- Dashboard summaries
- Social follow/block/follow-request flows

## Local Setup

### Requirements

- Java 21
- Docker Desktop or another local Docker runtime
- Maven Wrapper (`.\mvnw.cmd`) or Maven
- TMDB API bearer token

### Environment Variables

The application reads configuration from environment variables in `src/main/resources/application.properties`.

Core settings:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `TMDB_API_TOKEN`
- `APP_DOMAIN`
- `VERIFIED_SENDER`

Redis and cache settings:

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `WATCHMATE_CACHE_ENABLED`

Optional behavior toggles already supported by the backend:

- `WATCHMATE_DISCOVERY_SYNC_CRON`
- `WATCHMATE_DISCOVERY_SYNC_STARTUP_ENABLED`
- `WATCHMATE_SHOW_HYDRATION_MAX_SYNCHRONOUS_MISSING_SEASONS`
- `WATCHMATE_SHOW_HYDRATION_MAX_SYNCHRONOUS_EPISODES`
- `WATCHMATE_SHOW_HYDRATION_BATCH_SIZE`
- `WATCHMATE_SHOW_JOBS_ENABLED`
- `WATCHMATE_SHOW_JOBS_POLL_DELAY_MS`
- `WATCHMATE_SHOW_JOBS_MAX_JOBS_PER_POLL`
- `WATCHMATE_SHOW_JOBS_STALE_RUNNING_MINUTES`
- `WATCHMATE_SHOW_JOBS_MAX_ATTEMPTS`

AWS SES credentials are also required when running real email delivery for registration and verification flows.

### Docker Compose

The repo includes [docker-compose.yml](/C:/Users/forsu/OneDrive/Desktop/watchmate/docker-compose.yml), which starts:

- the Spring Boot app
- MySQL 8
- Redis 7

Typical flow:

```powershell
docker compose up --build
```

The API is exposed on `http://localhost:8080`.

## Current API Map

This is a backend-oriented map of the current API groups. It is intentionally high level and should not be treated as a frontend contract beyond what the backend currently exposes.

### Auth

Base path: `/api/v1/auth`

- `POST /register`
- `GET /verify?token=...`
- `POST /verify/resend`
- `POST /login`
- `POST /refresh`
- `POST /logout`

Notes:

- Login and refresh return a token DTO with `accessToken`, `refreshToken`, `accessTokenExpiry`, and `tokenType`.
- Some auth endpoints still return plain string bodies, including register, verify, resend verification, and logout.

### Public Discovery and Media

- `GET /api/v1/home`
- `GET /api/v1/home/status` (admin only)
- `GET /api/v1/discover/trending-movies`
- `GET /api/v1/discover/trending-shows`
- `GET /api/v1/discover/popular-now`
- `GET /api/v1/discover/airing-today`
- `GET /api/v1/discover/upcoming`
- `GET /api/v1/discover/recommended-later`
- `GET /api/v1/genre/{genre}/movies`
- `GET /api/v1/genre/{genre}/shows`
- `GET /api/v1/media/search`
- `GET /api/v1/movies/{tmdbId}`
- `GET /api/v1/shows/{tmdbId}`
- `GET /api/v1/shows/{tmdbId}/next-episode`
- `GET /api/v1/shows/{tmdbId}/seasons/{seasonNumber}/episodes`

Notes:

- Movie and show detail endpoints are public. When authenticated, they may include user-specific fields such as favourite or watch status.
- Show season episode responses now include nullable `tmdbEpisodeId` when hydrated.

### Tracking

Movie tracking:

- `PUT /api/v1/movies/{tmdbId}/status`

Show tracking:

- `PUT /api/v1/shows/{tmdbId}/status`
- `GET /api/v1/shows/{tmdbId}/progress`
- `PUT /api/v1/shows/{tmdbId}/progress`
- `GET /api/v1/shows/{tmdbId}/episodes/watched`
- `GET /api/v1/show-tracking-jobs/{jobId}`

Important show-progress notes:

- `PUT /api/v1/shows/{tmdbId}/progress` is the canonical manual show-progress endpoint.
- The backend replaces watched-episode rows with the exact contiguous prefix from episode `1x1` through the requested season/episode pointer.
- The endpoint can return `200 OK` when the update completes immediately.
- The endpoint can return `202 Accepted` when metadata hydration or completion continues in the background.
- On `202`, poll the URL in the `Location` header and respect the `Retry-After` header.

Removed and not current:

- No `PUT /api/v1/shows/{tmdbId}/episodes/{seasonNumber}/{episodeNumber}`
- No `PUT /api/v1/shows/{tmdbId}/seasons/{seasonNumber}/watched`

### Watchlists, Favourites, Reviews

Watchlists:

- `GET /api/v1/watchlists`
- `POST /api/v1/watchlists`
- `PATCH /api/v1/watchlists/{id}`
- `DELETE /api/v1/watchlists/{id}`
- `POST /api/v1/watchlists/{watchListId}/items/{tmdbId}`
- `DELETE /api/v1/watchlists/{watchListId}/items/{tmdbId}`

Favourites:

- `POST /api/v1/favourites/add/{tmdbId}`
- `DELETE /api/v1/favourites/remove/{tmdbId}`
- `GET /api/v1/favourites/all`
- `GET /api/v1/favourites/check/{tmdbId}`

Reviews:

- `POST /api/v1/reviews/create`
- `PATCH /api/v1/reviews/{reviewId}`
- `DELETE /api/v1/reviews/{reviewId}`
- `GET /api/v1/reviews/{reviewId}`
- `GET /api/v1/movies/{tmdbId}/reviews`
- `GET /api/v1/shows/{tmdbId}/reviews`

Notes:

- Watchlist and favourite item endpoints accept an optional `type` query parameter when a TMDB ID may be ambiguous or needs importing first.

### Dashboard

Base path: `/api/v1/dashboard`

- `GET /continue-watching`
- `GET /upcoming-episodes`
- `GET /calendar`

### Social

Base path: `/api/v1/social`

- `POST /follow/{userId}`
- `DELETE /unfollow/{userId}`
- `POST /follow-request/{requestId}/accept`
- `POST /follow-request/{requestId}/reject`
- `DELETE /follow-request/{requestId}/cancel`
- `GET /follow-requests/received`
- `GET /follow-status/{userId}`
- `POST /block/{userId}`
- `GET /followers-list`
- `GET /following-list`
- `GET /search`
- `GET /profile/{username}`

Notes:

- Private-user follow requests use `REQUESTED` as the lightweight follow status while the request is pending.
- Follow-request transitions are guarded by `PENDING`.
- Stale accept, reject, or cancel attempts return `409` with code `FOLLOW_REQUEST_STATE_CONFLICT`.
- Lightweight follow status priority is `BLOCKED` > `FOLLOWING` > `REQUESTED` > `NOT_FOLLOWING`.

## Response Conventions

- Login and refresh return a token DTO, not a generic envelope.
- Some endpoints return plain strings.
- Errors use the shape:

```json
{
  "message": "Human-readable message",
  "code": "STABLE_ERROR_CODE",
  "fields": []
}
```

- Pagination is not globally wrapped in one custom envelope. Some endpoints return Spring `Page<T>`, some return arrays, and some return dedicated DTOs like `ContinueWatchingResponseDTO` or `HomeResponseDTO`.

## Testing

Compile without tests:

```powershell
.\mvnw.cmd -q -DskipTests compile
```

Run the focused cleanup-related suite:

```powershell
.\mvnw.cmd -q "-Dtest=ReviewServiceTest,ReviewIntegrationTest,SocialServiceTest,SocialIntegrationTest,ShowTrackingServiceTest,ShowFeaturesIntegrationTest" test
```

Run the full suite:

```powershell
.\mvnw.cmd -q test
```

Notes:

- Integration tests use Testcontainers for MySQL.
- Some cache-specific tests also use Testcontainers-backed Redis.
- Docker must be available for the full integration suite to pass locally.

## Stale Endpoints Removed From This README

The following old references are intentionally no longer documented here because they are not current backend endpoints:

- `/api/v1/media/popular`
- `/api/v1/media/update`
- removed manual show episode/season mutation endpoints
