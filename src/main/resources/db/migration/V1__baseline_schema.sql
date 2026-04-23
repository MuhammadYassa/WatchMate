create table users (
    email_verified bit not null,
    id bigint not null auto_increment,
    email varchar(255) not null,
    password varchar(255) not null,
    username varchar(255) not null,
    privacy_status enum ('PRIVATE','PUBLIC'),
    role enum ('ADMIN','MODERATOR','USER') not null,
    constraint pk_users primary key (id),
    constraint uq_users_email unique (email),
    constraint uq_users_username unique (username)
) engine=InnoDB;

create table genre (
    id bigint not null,
    name varchar(255),
    constraint pk_genre primary key (id)
) engine=InnoDB;

create table media (
    rating float(53),
    release_date date,
    id bigint not null auto_increment,
    tmdb_id bigint not null,
    poster_path varchar(255),
    title varchar(255),
    overview longtext,
    type enum ('MOVIE','SHOW'),
    constraint pk_media primary key (id),
    constraint uq_media_tmdb_id unique (tmdb_id)
) engine=InnoDB;

create table blocked_users (
    blocked_id bigint not null,
    blocker_id bigint not null,
    constraint pk_blocked_users primary key (blocker_id, blocked_id)
) engine=InnoDB;

create table email_verification_token (
    expires_at datetime(6) not null,
    id bigint not null auto_increment,
    user_id bigint not null,
    token varchar(255) not null,
    constraint pk_email_verification_token primary key (id),
    constraint uq_email_verification_token_user unique (user_id)
) engine=InnoDB;

create table follow_request (
    id bigint not null auto_increment,
    request_user_id bigint not null,
    requested_at datetime(6) not null,
    responded_at datetime(6),
    target_user_id bigint not null,
    status enum ('ACCEPTED','CANCELED','PENDING','REJECTED') not null,
    constraint pk_follow_request primary key (id)
) engine=InnoDB;

create table media_genres (
    genre_id bigint not null,
    media_id bigint not null,
    constraint pk_media_genres primary key (media_id, genre_id)
) engine=InnoDB;

create table popular_media (
    popularity_rank integer,
    id bigint not null auto_increment,
    media_id bigint not null,
    constraint pk_popular_media primary key (id),
    constraint uq_popular_media_media unique (media_id)
) engine=InnoDB;

create table refresh_tokens (
    revoked bit not null,
    created_at datetime(6) not null,
    expiry_date datetime(6) not null,
    id bigint not null auto_increment,
    user_id bigint not null,
    token varchar(255) not null,
    constraint pk_refresh_tokens primary key (id),
    constraint uq_refresh_tokens_token unique (token)
) engine=InnoDB;

create table review (
    rating integer not null check ((rating <= 5) and (rating >= 1)),
    date_last_modified datetime(6),
    date_posted datetime(6),
    id bigint not null auto_increment,
    media_id bigint not null,
    user_id bigint not null,
    comment varchar(255),
    constraint pk_review primary key (id),
    constraint uq_review_user_media unique (user_id, media_id)
) engine=InnoDB;

create table user_favorites (
    favorites_id bigint not null,
    users_id bigint not null,
    constraint pk_user_favorites primary key (users_id, favorites_id)
) engine=InnoDB;

create table user_following (
    follower_id bigint not null,
    following_id bigint not null,
    constraint pk_user_following primary key (follower_id, following_id)
) engine=InnoDB;

create table user_media_status (
    id bigint not null auto_increment,
    media_id bigint not null,
    user_id bigint not null,
    status enum ('NONE','TO_WATCH','WATCHED','WATCHING') not null,
    constraint pk_user_media_status primary key (id),
    constraint uq_user_media_status_user_media unique (user_id, media_id)
) engine=InnoDB;

create table watch_list (
    id bigint not null auto_increment,
    user_id bigint not null,
    name varchar(255) not null,
    constraint pk_watch_list primary key (id)
) engine=InnoDB;

create table watchlist_items (
    position integer,
    added_at datetime(6),
    id bigint not null auto_increment,
    media_id bigint not null,
    watchlist_id bigint not null,
    constraint pk_watchlist_items primary key (id),
    constraint uq_watchlist_items_watchlist_media unique (watchlist_id, media_id)
) engine=InnoDB;

alter table blocked_users
    add constraint fk_blocked_users_blocked foreign key (blocked_id) references users (id);

alter table blocked_users
    add constraint fk_blocked_users_blocker foreign key (blocker_id) references users (id);

alter table email_verification_token
    add constraint fk_email_verification_token_user foreign key (user_id) references users (id);

alter table follow_request
    add constraint fk_follow_request_request_user foreign key (request_user_id) references users (id);

alter table follow_request
    add constraint fk_follow_request_target_user foreign key (target_user_id) references users (id);

alter table media_genres
    add constraint fk_media_genres_genre foreign key (genre_id) references genre (id);

alter table media_genres
    add constraint fk_media_genres_media foreign key (media_id) references media (id);

alter table popular_media
    add constraint fk_popular_media_media foreign key (media_id) references media (id);

alter table refresh_tokens
    add constraint fk_refresh_tokens_user foreign key (user_id) references users (id);

alter table review
    add constraint fk_review_media foreign key (media_id) references media (id);

alter table review
    add constraint fk_review_user foreign key (user_id) references users (id);

alter table user_favorites
    add constraint fk_user_favorites_media foreign key (favorites_id) references media (id);

alter table user_favorites
    add constraint fk_user_favorites_user foreign key (users_id) references users (id);

alter table user_following
    add constraint fk_user_following_following foreign key (following_id) references users (id);

alter table user_following
    add constraint fk_user_following_follower foreign key (follower_id) references users (id);

alter table user_media_status
    add constraint fk_user_media_status_media foreign key (media_id) references media (id);

alter table user_media_status
    add constraint fk_user_media_status_user foreign key (user_id) references users (id);

alter table watch_list
    add constraint fk_watch_list_user foreign key (user_id) references users (id);

alter table watchlist_items
    add constraint fk_watchlist_items_media foreign key (media_id) references media (id);

alter table watchlist_items
    add constraint fk_watchlist_items_watchlist foreign key (watchlist_id) references watch_list (id);
