create table user_show_progress (
    episodes_watched_count integer not null default 0,
    seasons_completed_count integer not null default 0,
    current_episode_number integer null,
    current_season_number integer null,
    watch_position_episode integer null,
    watch_position_season integer null,
    id bigint not null auto_increment,
    media_id bigint not null,
    user_id bigint not null,
    last_watched_at datetime(6) null,
    constraint pk_user_show_progress primary key (id),
    constraint uq_user_show_progress_user_media unique (user_id, media_id)
) engine=InnoDB;

alter table user_show_progress
    add constraint fk_user_show_progress_media
    foreign key (media_id) references media (id);

alter table user_show_progress
    add constraint fk_user_show_progress_user
    foreign key (user_id) references users (id);

insert into user_show_progress (
    user_id,
    media_id,
    current_season_number,
    current_episode_number,
    episodes_watched_count,
    seasons_completed_count,
    last_watched_at
)
select
    ums.user_id,
    ums.media_id,
    ums.current_season_number,
    ums.current_episode_number,
    ums.episodes_watched_count,
    ums.seasons_completed_count,
    ums.last_watched_at
from user_media_status ums
join media m on m.id = ums.media_id
where m.type = 'SHOW'
  and (
      ums.current_season_number is not null
      or ums.current_episode_number is not null
      or ums.episodes_watched_count <> 0
      or ums.seasons_completed_count <> 0
      or ums.last_watched_at is not null
      or exists (
          select 1
          from user_episode_progress uep
          where uep.user_media_status_id = ums.id
      )
  );

alter table user_episode_progress
    add column user_show_progress_id bigint null;

update user_episode_progress uep
join user_media_status ums on ums.id = uep.user_media_status_id
join user_show_progress usp on usp.user_id = ums.user_id and usp.media_id = ums.media_id
set uep.user_show_progress_id = usp.id;

alter table user_episode_progress
    modify column user_show_progress_id bigint not null;

alter table user_episode_progress
    drop foreign key fk_user_episode_progress_status;

alter table user_episode_progress
    drop index uq_user_episode_progress_status_season_episode;

alter table user_episode_progress
    drop column user_media_status_id;

alter table user_episode_progress
    add constraint uq_user_episode_progress_progress_season_episode
        unique (user_show_progress_id, season_number, episode_number);

alter table user_episode_progress
    add constraint fk_user_episode_progress_show_progress
    foreign key (user_show_progress_id) references user_show_progress (id);

alter table user_media_status
    drop column current_season_number,
    drop column current_episode_number,
    drop column episodes_watched_count,
    drop column seasons_completed_count,
    drop column last_watched_at;
