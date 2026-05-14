create table show_season (
    season_number integer not null,
    episode_count integer not null,
    air_date date,
    created_at datetime(6) not null,
    synced_at datetime(6),
    updated_at datetime(6) not null,
    id bigint not null auto_increment,
    media_id bigint not null,
    name varchar(255),
    poster_path varchar(255),
    overview longtext,
    constraint pk_show_season primary key (id),
    constraint uq_show_season_media_season unique (media_id, season_number)
) engine=InnoDB;

create table show_episode (
    season_number integer not null,
    episode_number integer not null,
    runtime integer,
    air_date date,
    created_at datetime(6) not null,
    synced_at datetime(6),
    updated_at datetime(6) not null,
    id bigint not null auto_increment,
    media_id bigint not null,
    still_path varchar(255),
    title varchar(255),
    overview longtext,
    constraint pk_show_episode primary key (id),
    constraint uq_show_episode_media_season_episode unique (media_id, season_number, episode_number)
) engine=InnoDB;

create index idx_show_season_media_season on show_season (media_id, season_number);
create index idx_show_episode_media_season on show_episode (media_id, season_number);
create index idx_show_episode_media_season_episode on show_episode (media_id, season_number, episode_number);
create index idx_show_episode_air_date on show_episode (air_date);

alter table show_season
    add constraint fk_show_season_media foreign key (media_id) references media (id);

alter table show_episode
    add constraint fk_show_episode_media foreign key (media_id) references media (id);
