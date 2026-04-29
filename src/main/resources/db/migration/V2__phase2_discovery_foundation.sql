alter table media
    add column backdrop_path varchar(255);

alter table media
    modify column type enum ('MOVIE','SHOW') not null;

alter table media
    drop index uq_media_tmdb_id;

alter table media
    add constraint uq_media_tmdb_id_type unique (tmdb_id, type);

create table curated_content (
    rank_position integer not null,
    id bigint not null auto_increment,
    media_id bigint not null,
    synced_at datetime(6) not null,
    category_key enum (
        'AIRING_TODAY',
        'POPULAR_NOW',
        'RECOMMENDED_LATER',
        'TRENDING_MOVIES',
        'TRENDING_SHOWS',
        'UPCOMING'
    ) not null,
    media_type enum ('MOVIE','SHOW') not null,
    constraint pk_curated_content primary key (id),
    constraint uq_curated_content_category_rank unique (category_key, rank_position),
    constraint uq_curated_content_category_media unique (category_key, media_id)
) engine=InnoDB;

create table genre_lookup (
    id bigint not null auto_increment,
    synced_at datetime(6) not null,
    tmdb_genre_id bigint not null,
    name varchar(255) not null,
    media_type enum ('MOVIE','SHOW') not null,
    constraint pk_genre_lookup primary key (id),
    constraint uq_genre_lookup_tmdb_genre_type unique (tmdb_genre_id, media_type),
    constraint uq_genre_lookup_name_type unique (name, media_type)
) engine=InnoDB;

create table content_sync_status (
    airing_today_count integer,
    popular_now_count integer,
    recommended_later_count integer,
    trending_movies_count integer,
    trending_shows_count integer,
    upcoming_count integer,
    last_attempted_at datetime(6),
    last_failed_at datetime(6),
    last_successful_at datetime(6),
    status_key varchar(50) not null,
    last_error_message varchar(1000),
    last_result enum ('FAILURE','NEVER','SUCCESS') not null,
    constraint pk_content_sync_status primary key (status_key)
) engine=InnoDB;

alter table curated_content
    add constraint fk_curated_content_media foreign key (media_id) references media (id);
