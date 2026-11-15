<div align="center">

# 🎬 WatchMate — Backend

**The API behind a full-stack movie & TV tracking platform.**

Track what you've watched, remember where you left off, and discover what to watch next.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

**[🌐 Live App](https://www.mywatchmate.us)** · **[💻 Frontend Repository](https://github.com/MuhammadYassa/WatchMate_Frontend)** · **[👤 Author](https://github.com/MuhammadYassa)**

</div>

---

## About the project

**WatchMate** is a solo-built and deployed movie and television tracking application. The backend powers authentication, movie and TV discovery, watch progress, watchlists, reviews, personalized dashboards, and social features. It integrates with [TMDB](https://www.themoviedb.org/) for catalog metadata while maintaining its own application data in MySQL.

Over a two-month milestone, the product reached **100 users**, with **4,298 episodes tracked**. An earlier three-week beta involved 10 users and 1,148 episode watches.

This repository contains the **Spring Boot API**. The React/TypeScript interface lives in the [frontend repository](https://github.com/MuhammadYassa/WatchMate_Frontend).

### Engineering highlights

| Area | Implementation |
| --- | --- |
| **Authentication** | Spring Security, JWT access tokens, hashed and atomically rotated refresh tokens, and Bucket4j rate limiting |
| **Background processing** | Asynchronous, persisted show-tracking jobs for metadata hydration and large multi-season imports |
| **Performance** | Redis caching; movie-detail benchmark improved from **420 ms to 28 ms**, with **80% cache hits** and **75% fewer TMDB requests** in a 10,000-request test |
| **Data integrity** | Spring Data JPA, MySQL, Flyway migrations, and service-layer validation |
| **Testing** | JUnit and Testcontainers integration tests; approximately **90% line coverage** reported for the tested backend |
| **Delivery** | Docker Compose, GitHub Actions, and deployment on an Oracle Cloud Infrastructure VM |

> Performance and adoption figures are project milestones / benchmarks, not live counters. Results may vary with hardware, cache state, and request mix.

---

## Features

- **Accounts & security:** registration, email verification, login, token refresh, logout, and authorization.
- **Media discovery:** trending, popular, upcoming, and genre-based browsing; search and details powered by TMDB.
- **Movie & TV tracking:** movie statuses, show statuses, watched episodes, and contiguous season/episode progress.
- **Personal libraries:** watchlists, favourites, and reviews.
- **Dashboard:** continue watching, upcoming episodes, and calendar data.
- **Social:** profiles, users, followers, private follow requests, and blocking.
- **Efficient metadata access:** local catalog persistence, Redis caches, and asynchronous hydration when requests would otherwise take too long.

## Architecture

```text
React / TypeScript client
          |
          v
   Spring Boot REST API
          |
   +------+--------------------------+
   |      |                          |
   v      v                          v
Security  Application services    TMDB integration
 & JWT    (tracking, social,       (catalog & metadata)
          discovery, reviews)
              |          |
              v          v
          MySQL/JPA     Redis cache
              |
              v
     Persisted background jobs
     + scheduled job worker
```

The service is organized into **controllers → services → repositories**, with separate entities and DTOs. Controllers expose REST endpoints and OpenAPI annotations; services handle business rules, orchestration, caching, and async decisions; Spring Data JPA repositories handle persistence.

### Three design decisions worth highlighting

**1. Move expensive TV imports off request threads.** Importing a show with many seasons can require substantial metadata work. When synchronous processing isn't appropriate, the backend accepts the work with **`202 Accepted`** and a job location. The client can poll for completion instead of holding an HTTP request open.

**2. Cache frequently requested metadata.** Redis reduces repeated external API calls and response latency. In a 10,000-request benchmark, cache hit rate reached **80%**, with **75% fewer TMDB calls**; a movie-detail response dropped from **420 ms to 28 ms**.

**3. Treat authentication as more than login.** The API uses short-lived JWT access tokens, hashed refresh-token storage, atomic rotation on refresh, and endpoint rate limits to reduce common session and abuse risks.

---

## API overview

The API is organized under `/api/v1`. Selected endpoints from the current implementation:

| Area | Example endpoints |
| --- | --- |
| **Auth** | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| **Discovery** | `GET /discover/trending-movies`, `GET /discover/trending-shows`, `GET /media/search` |
| **Details** | `GET /movies/{tmdbId}`, `GET /shows/{tmdbId}` |
| **Movie tracking** | `PUT /movies/{tmdbId}/status` |
| **Show progress** | `GET /shows/{tmdbId}/progress`, `PUT /shows/{tmdbId}/progress` |
| **Background jobs** | `GET /show-tracking-jobs/{jobId}` |
| **Watchlists** | `GET /watchlists`, `POST /watchlists`, `PATCH /watchlists/{id}` |
| **Reviews** | `POST /reviews/create`, `GET /movies/{tmdbId}/reviews` |
| **Dashboard** | `GET /dashboard/continue-watching`, `GET /dashboard/calendar` |
| **Social** | `POST /social/follow/{userId}`, `GET /social/profile/{username}` |

### How asynchronous show tracking works

```text
PUT /api/v1/shows/{tmdbId}/progress
   |
   +-- 200 OK ----------> progress updated synchronously
   |
   +-- 202 Accepted ----> background work queued
                              |
                         Location header: job URL
                         Retry-After header: polling guidance
                              |
                         GET /api/v1/show-tracking-jobs/{jobId}
```

Manual show progress is represented as the contiguous watched prefix ending at the selected season and episode. The server may complete that update immediately or continue metadata hydration and completion in a background job.

Error responses use this shape where applicable:

```json
{
  "message": "Human-readable message",
  "code": "STABLE_ERROR_CODE",
  "fields": []
}
```

Not every endpoint uses a common response envelope: authentication endpoints can return token DTOs or strings, and other endpoints return their own DTOs, lists, or paginated results.

---

## Run locally

### Prerequisites

- **Docker** with Docker Compose
- A **TMDB API bearer token**
- **Java 21** if running Maven commands outside containers
- Email-service configuration if you want to exercise registration and verification emails (the project uses AWS SES)

### 1. Clone and configure

```bash
git clone https://github.com/MuhammadYassa/WatchMate.git
cd WatchMate
cp .env.example .env
```

On Windows PowerShell, use `Copy-Item .env.example .env` instead of `cp` if preferred.

Edit `.env` before starting. In particular, set your own `JWT_SECRET`, `TMDB_API_TOKEN`, database passwords, `APP_DOMAIN=http://localhost:5173`, and email-related values as needed. **Do not commit your `.env` or real credentials.**

### 2. Start the development stack

```bash
docker compose -f docker-compose.dev.yml up --build
```

The **development** Compose file launches the Spring Boot API, MySQL, and Redis together and exposes the API at **`http://localhost:8080`**.

The separate [`docker-compose.yml`](./docker-compose.yml) is for the deployment arrangement with an externally configured database; it is **not** the same all-in-one local development stack.

### 3. Connect the frontend

In the [frontend repository](https://github.com/MuhammadYassa/WatchMate_Frontend), create `.env.local` with:

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Then run the React app on its usual Vite development address (`http://localhost:5173`). See the frontend README for details.

### 4. Stop the stack

```bash
docker compose -f docker-compose.dev.yml down
```

> The development Compose file includes named volumes for MySQL and Redis. Do not add `-v` to `down` unless you intentionally want to remove stored development data.

---

## Testing

This repository includes JUnit tests and Testcontainers-backed integration tests for MySQL and, in some cases, Redis. **Docker must be running** for the full integration suite.

macOS / Linux:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

The Maven build is also configured to generate a **JaCoCo coverage report** under `target/site/jacoco/` when tests run.

### Key technologies

`Java 21` · `Spring Boot 4` · `Spring Security` · `Spring Data JPA` · `Flyway` · `MySQL` · `Redis` · `JWT` · `Bucket4j` · `TMDB API` · `Scheduled background jobs` · `JUnit` · `Testcontainers` · `Docker` · `GitHub Actions` · `OCI`

---

## Related links

- **Try the product:** https://www.mywatchmate.us
- **Frontend source:** https://github.com/MuhammadYassa/WatchMate_Frontend
- **Developer:** [Muhammad Yassa](https://github.com/MuhammadYassa) · [LinkedIn](https://www.linkedin.com/in/muhammadyassa/)

<sub>Movie and TV metadata is provided through the TMDB integration. WatchMate is an independent project, not an official TMDB product.</sub>
