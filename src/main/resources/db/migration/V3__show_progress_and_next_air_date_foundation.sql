alter table user_media_status
    add column current_season_number integer null,
    add column current_episode_number integer null,
    add column episodes_watched_count integer not null default 0,
    add column seasons_completed_count integer not null default 0,
    add column last_watched_at datetime(6) null;

alter table media
    add column next_episode_air_date date null,
    add column next_episode_season_number integer null,
    add column next_episode_episode_number integer null,
    add column next_episode_name varchar(255) null,
    add column last_episode_to_air_season_number integer null,
    add column last_episode_to_air_episode_number integer null,
    add column last_episode_to_air_name varchar(255) null,
    add column last_air_date date null,
    add column number_of_seasons integer null,
    add column number_of_episodes integer null,
    add column tmdb_show_status varchar(100) null,
    add column next_airing_synced_at datetime(6) null;

create table user_episode_progress (
    watched bit not null,
    watched_at datetime(6),
    id bigint not null auto_increment,
    user_media_status_id bigint not null,
    season_number integer not null,
    episode_number integer not null,
    constraint pk_user_episode_progress primary key (id),
    constraint uq_user_episode_progress_status_season_episode
        unique (user_media_status_id, season_number, episode_number)
) engine=InnoDB;

alter table user_episode_progress
    add constraint fk_user_episode_progress_status
    foreign key (user_media_status_id) references user_media_status (id);
