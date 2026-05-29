create table genre_typed (
    id bigint not null auto_increment,
    synced_at datetime(6) not null,
    tmdb_genre_id bigint not null,
    name varchar(255) not null,
    media_type enum ('MOVIE','SHOW') not null,
    constraint pk_genre_typed primary key (id),
    constraint uq_genre_typed_tmdb_genre_type unique (tmdb_genre_id, media_type),
    constraint uq_genre_typed_name_type unique (name, media_type)
) engine=InnoDB;

insert into genre_typed (synced_at, tmdb_genre_id, name, media_type)
select
    genre_lookup.synced_at,
    genre_lookup.tmdb_genre_id,
    genre_lookup.name,
    genre_lookup.media_type
from genre_lookup;

insert ignore into genre_typed (synced_at, tmdb_genre_id, name, media_type)
select
    current_timestamp(6),
    legacy_genre.id,
    legacy_genre.name,
    media.type
from genre legacy_genre
join media_genres legacy_media_genres on legacy_media_genres.genre_id = legacy_genre.id
join media on media.id = legacy_media_genres.media_id;

create table media_genres_typed (
    genre_id bigint not null,
    media_id bigint not null,
    constraint pk_media_genres_typed primary key (media_id, genre_id),
    constraint uq_media_genres_typed_media_genre unique (media_id, genre_id)
) engine=InnoDB;

insert ignore into media_genres_typed (media_id, genre_id)
select
    legacy_media_genres.media_id,
    genre_typed.id
from media_genres legacy_media_genres
join genre legacy_genre on legacy_genre.id = legacy_media_genres.genre_id
join media on media.id = legacy_media_genres.media_id
join genre_typed on genre_typed.tmdb_genre_id = legacy_genre.id
    and genre_typed.media_type = media.type;

drop table media_genres;
drop table genre_lookup;
drop table genre;

rename table genre_typed to genre;
rename table media_genres_typed to media_genres;

alter table genre
    rename index uq_genre_typed_tmdb_genre_type to uq_genre_tmdb_genre_type;

alter table genre
    rename index uq_genre_typed_name_type to uq_genre_name_type;

alter table media_genres
    rename index uq_media_genres_typed_media_genre to uq_media_genres_media_genre;

alter table media_genres
    add constraint fk_media_genres_media foreign key (media_id) references media (id);

alter table media_genres
    add constraint fk_media_genres_genre foreign key (genre_id) references genre (id);
