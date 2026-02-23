WatchMate – Movie & TV Watchlist & Social Backend
=================================================

WatchMate is a Spring Boot REST API that powers a movie / TV tracking experience similar to Letterboxd.  
It lets users discover media, maintain watchlists and favourites, write reviews, and follow other users – all backed by JWT authentication, MySQL, and TMDB data.

### Tech Stack

- **Language**: Java 21  
- **Framework**: Spring Boot 4 (Web, Security, Data JPA, Validation, Actuator, WebFlux)  
- **Database**: MySQL  
- **Auth**: JWT (access + refresh tokens), Spring Security, BCrypt  
- **Messaging / Email**: AWS SES for email verification  
- **External API**: TMDB (The Movie Database) for media data  
- **Build Tool**: Maven  

---

## Features

- **User authentication & security**
  - Registration with **email verification** (verification tokens via email).
  - Login with **JWT-based authentication** and refresh tokens.
  - Logout and refresh token revocation.
  - Stateless security configuration using Spring Security and custom JWT filter.

- **Media discovery**
  - Search movies/TV by query via TMDB (`/api/v1/media/search`).
  - Fetch media details by TMDB ID and type (`/api/v1/media/{tmdbId}?type=...`).
  - Retrieve **popular media** list from a pre-populated table (`/api/v1/media/popular`).

- **Watchlists**
  - Create / rename / delete user-specific watchlists.
  - Add and remove media items from watchlists.
  - Retrieve all watchlists for the authenticated user.

- **Favourites**
  - Add/remove favourites.
  - Check if a media item is favourited.
  - Get a consolidated view of a user’s favourites.

- **Reviews**
  - Create, update, delete, and fetch reviews for media.
  - Get all reviews for a specific media item.
  - Validation and ownership checks for review operations.

- **Social graph**
  - Follow / unfollow users.
  - Follow request flow with **accept / reject / cancel**.
  - Followers / following paginated lists.
  - Block user support.
  - Public **user profile** endpoint backed by social data.

- **Status tracking**
  - Per-user media status (e.g. watched / watching / planned) via `/api/v1/media/update`.

- **Production-ready practices**
  - Layered architecture (**Controller → Service → Repository → Model/DTO**).
  - DTO-based request/response contracts with bean validation.
  - Custom exceptions and centralized error handling.
  - Unit tests for key services (JWT, user, search, TMDB, etc.).
  - Actuator starter included for future health/metrics endpoints.

---

## Project Structure (High Level)

- `src/main/java/com/project/watchmate`
  - `Controllers` – REST controllers (`UserController`, `MediaController`, `WatchListController`, `FavouriteController`, `ReviewController`, `SocialController`).
  - `Services` – business logic (auth, JWT, social, favourites, reviews, search, TMDB integration, watchlists, status, email verification).
  - `Repositories` – Spring Data JPA repositories for `Users`, `Media`, `PopularMedia`, `Genres`, `WatchList`, `Reviews`, etc.
  - `Models` – JPA entities and supporting enums (`Users`, `Media`, `WatchListItem`, `Review`, `MediaType`, `WatchStatus`, etc.).
  - `Dto` – request and response DTOs used by controllers.
  - `Config` – security, AWS SES, WebClient configuration.
  - `Exception` – custom exception types and auth entry point.
- `src/main/resources/application.properties` – configuration via environment variables.
- `src/test/java/com/project/watchmate` – unit tests for core services and integrations.

---

## Getting Started

### Prerequisites

- **JDK 21+**
- **Maven 3.9+**
- **MySQL 8+** (or compatible)
- An **AWS SES** account with a verified sender email.
- A **TMDB API token** (v4 Bearer token).

### 1. Clone the repository

```bash
git clone <repo-url>
cd watchmate
```

### 2. Configure environment

The app is configured via environment variables that are read in `application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO}
spring.jpa.show-sql=${JPA_SHOW_SQL:false}

jwt.secret=${JWT_SECRET}

tmdb.api.token=${TMDB_API_TOKEN}

app.domain=${APP_DOMAIN}
verified.sender=${VERIFIED_SENDER}
```

Set these in your shell or IDE run configuration, for example:

```bash
setx DB_URL "jdbc:mysql://localhost:3306/watchmate"
setx DB_USERNAME "watchmate_user"
setx DB_PASSWORD "strong-password"
setx JPA_DDL_AUTO "update"
setx JWT_SECRET "a-strong-random-secret"
setx TMDB_API_TOKEN "<your-tmdb-bearer-token>"
setx APP_DOMAIN "http://localhost:8080"
setx VERIFIED_SENDER "no-reply@yourdomain.com"
```

For non-Windows environments, use `export` instead of `setx`.

> **Note**: For local development you can use `JPA_DDL_AUTO=update` or `create-drop`. In production, prefer managed migrations (e.g. Flyway) and set this to `none`.

### 3. Create the database

In MySQL:

```sql
CREATE DATABASE watchmate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'watchmate_user'@'%' IDENTIFIED BY 'strong-password';
GRANT ALL PRIVILEGES ON watchmate.* TO 'watchmate_user'@'%';
FLUSH PRIVILEGES;
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 5. Run tests

```bash
mvn test
```

---

## API Overview (High Level)

This is a high-level summary; see controllers and DTOs for full details.

- **Auth (`/api/v1/auth`)**
  - `POST /register` – register a new user and send verification email.
  - `GET /verify?token=...` – verify email.
  - `POST /verify/resend` – resend verification email.
  - `POST /login` – authenticate and receive access/refresh tokens.
  - `POST /refresh` – exchange refresh token for new access token.
  - `POST /logout` – revoke refresh token.

- **Media (`/api/v1/media`)**
  - `GET /{tmdbId}?type=...` – media details for a given TMDB ID and type.
  - `GET /search?query=...&page=...` – search movies/TV.
  - `GET /popular` – list popular media from `PopularMedia`.
  - `POST /update` – update current user’s watch status for a media item.
  - `GET /{mediaId}/reviews` – get reviews for a given media.

- **Watchlists (`/api/v1/watchlists`)**
  - `GET /` – all watchlists for current user.
  - `POST /` – create a new watchlist.
  - `PATCH /{id}` – rename a watchlist.
  - `DELETE /{id}` – delete a watchlist.
  - `POST /{watchListId}/items/{tmdbId}` – add media to watchlist.
  - `DELETE /{watchListId}/items/{tmdbId}` – remove media from watchlist.

- **Favourites (`/api/v1/favourites`)**
  - `POST /add/{tmdbId}` – add favourite.
  - `DELETE /remove/{tmdbId}` – remove favourite.
  - `GET /all` – list favourites for user.
  - `GET /check/{tmdbId}` – check if a media is favourited.

- **Reviews (`/api/v1/reviews`)**
  - `POST /create` – create review.
  - `PATCH /{reviewId}` – update review.
  - `DELETE /{reviewId}` – delete review.
  - `GET /{reviewId}` – get single review.

- **Social (`/api/v1/social`)**
  - `POST /follow/{userId}` / `DELETE /unfollow/{userId}` – follow/unfollow.
  - `POST /follow-request/{requestId}/accept|reject` – handle incoming follow requests.
  - `DELETE /follow-request/{requestId}/cancel` – cancel outgoing follow request.
  - `GET /follow-requests/received` – paginated list of received requests.
  - `GET /follow-status/{userId}` – current follow/block status vs another user.
  - `POST /block/{userId}` – block a user.
  - `GET /followers-list` / `GET /following-list` – paginated followers/following lists.
  - `GET /user-profile/{userId}` – public profile view.

All non-auth endpoints require a valid JWT in the `Authorization: Bearer <token>` header as configured in `SecurityConfig`.

---

## Design & Implementation Notes

- **Security**
  - Stateless, JWT-based auth with a custom `JwtFilter` added before `UsernamePasswordAuthenticationFilter`.
  - `BCryptPasswordEncoder` with strength 12 for password hashing.
  - Custom `AuthenticationEntryPoint` for clean 401 responses.
  - Only auth endpoints (`/api/v1/auth/...`) are publicly accessible; everything else requires authentication.

- **Persistence**
  - Spring Data JPA repositories for all aggregates.
  - Entities model media, users, watchlists, favourites, reviews, follow requests, and user-media status.

- **External integrations**
  - **TMDB** via a dedicated `TmdbClient` built on Spring WebClient.
  - **AWS SES** with dedicated config for sending verification emails.

- **Testing**
  - Unit tests for critical services such as:
    - `JwtService`
    - `UserService`
    - `SearchService`
    - `TmdbService`
    - `EmailVerificationTokenService`
    - `RefreshTokenService`

---

## Deployment Notes (High Level)

- Package as an executable JAR:

```bash
mvn clean package
```

- Provide all required environment variables (`DB_*`, `JWT_SECRET`, `TMDB_API_TOKEN`, `APP_DOMAIN`, `VERIFIED_SENDER`) on the target environment.
- Behind a reverse proxy (Nginx/Apache/API gateway), expose port `8080` and terminate TLS at the proxy or your cloud provider.
- For production, configure:
  - A dedicated MySQL instance.
  - Proper AWS IAM permissions for SES.
  - Application logging, monitoring (via Actuator), and backups.

---

## Possible Future Improvements

- API documentation via **OpenAPI/Swagger**.
- Integration tests with Testcontainers for MySQL and TMDB stubs.
- Rate limiting and abuse protection.
- More granular privacy controls on watchlists / profiles.
- Frontend client (web or mobile) consuming this API.

---

## License

This project does not currently declare a license.  
If you plan to open source or share it publicly, consider adding a license (e.g. MIT, Apache 2.0) to clarify usage rights.

# WatchMate

**WatchMate** is a comprehensive web application designed to help users effortlessly track, rate, and review movies and TV shows. Whether you’re planning what to watch next, keeping tabs on what you’ve watched, or discovering new favorites, WatchMate makes managing your entertainment simple and personalized.

---

## Key Features

- User registration and secure login with JWT-based authentication  
- Manage personalized watchlists for movies and TV shows  
- Track viewing progress, mark titles as watched or currently watching  
- Rate and review your favorite content  
- Personalized recommendations based on your watch history and preferences  
- Responsive frontend interface for an intuitive user experience

---

## Technology Stack

### Backend
- Spring Boot framework with RESTful API  
- Spring Security for authentication and authorization  
- JWT (JSON Web Tokens) for stateless, secure sessions  
- MySQL relational database for data persistence  
- Hibernate (JPA) for ORM and database interactions

---

## Getting Started

### Prerequisites
- Java 17 or later    
- MySQL installed and running locally or accessible remotely  
- Maven for backend build  

### Setup Instructions

1. **Clone the repository:**  
   `git clone https://github.com/YourUsername/WatchMate.git`

2. **Configure backend:**  
   - Edit the `application-template.properties` file with your MySQL database credentials and JWT secret key and rename to `application.properties`. 
   - Build and run the backend server:  
     `./mvnw spring-boot:run`  

3. **Setup frontend**  

4. **Access the application**  

---

## License

This project is licensed under the MIT License.

---

## Author

Muhammad Yassa
