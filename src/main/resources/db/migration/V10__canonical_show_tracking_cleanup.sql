alter table media
    add column last_tmdb_sync_at datetime(6) null;

alter table show_season
    change column synced_at last_tmdb_sync_at datetime(6) null;

alter table show_episode
    change column synced_at last_tmdb_sync_at datetime(6) null;

alter table user_show_progress
    add column status enum ('TO_WATCH','WATCHING','UP_TO_DATE','WATCHED') null after media_id,
    add column created_at datetime(6) null after last_watched_at,
    add column updated_at datetime(6) null after created_at;

insert into user_show_progress (
    user_id,
    media_id,
    status,
    watch_position_episode,
    watch_position_season,
    episodes_watched_count,
    seasons_completed_count,
    last_watched_at,
    created_at,
    updated_at
)
select
    ums.user_id,
    ums.media_id,
    case ums.status
        when 'TO_WATCH' then 'TO_WATCH'
        when 'WATCHING' then 'WATCHING'
        when 'UP_TO_DATE' then 'UP_TO_DATE'
        when 'WATCHED' then 'WATCHED'
        else null
    end,
    null,
    null,
    0,
    0,
    null,
    coalesce(ums.updated_at, current_timestamp(6)),
    coalesce(ums.updated_at, current_timestamp(6))
from user_media_status ums
join media m on m.id = ums.media_id
left join user_show_progress usp on usp.user_id = ums.user_id and usp.media_id = ums.media_id
where m.type = 'SHOW'
  and ums.status <> 'NONE'
  and usp.id is null;

update user_show_progress usp
left join user_media_status ums on ums.user_id = usp.user_id and ums.media_id = usp.media_id
left join media m on m.id = usp.media_id
left join (
    select distinct user_show_progress_id
    from user_episode_progress
) uep on uep.user_show_progress_id = usp.id
set
    usp.status = coalesce(
        case
            when m.type = 'SHOW' then
                case ums.status
                    when 'TO_WATCH' then 'TO_WATCH'
                    when 'WATCHING' then 'WATCHING'
                    when 'UP_TO_DATE' then 'UP_TO_DATE'
                    when 'WATCHED' then 'WATCHED'
                    else null
                end
            else usp.status
        end,
        case
            when usp.watch_position_season is not null
                or usp.watch_position_episode is not null
                or usp.current_season_number is not null
                or usp.current_episode_number is not null
                or usp.episodes_watched_count > 0
                or usp.seasons_completed_count > 0
                or usp.last_watched_at is not null
                or uep.user_show_progress_id is not null
            then 'WATCHING'
            else 'TO_WATCH'
        end
    ),
    usp.created_at = coalesce(usp.created_at, coalesce(ums.updated_at, current_timestamp(6))),
    usp.updated_at = coalesce(usp.updated_at, coalesce(ums.updated_at, current_timestamp(6)));

alter table user_show_progress
    modify column status enum ('TO_WATCH','WATCHING','UP_TO_DATE','WATCHED') not null,
    modify column created_at datetime(6) not null,
    modify column updated_at datetime(6) not null;

delete from user_episode_progress
where watched = 0;

alter table user_episode_progress
    add column created_at datetime(6) null after watched_at,
    add column updated_at datetime(6) null after created_at;

update user_episode_progress
set
    created_at = coalesce(created_at, coalesce(watched_at, current_timestamp(6))),
    updated_at = coalesce(updated_at, coalesce(watched_at, current_timestamp(6)));

alter table user_episode_progress
    modify column created_at datetime(6) not null,
    modify column updated_at datetime(6) not null;

alter table user_show_progress
    drop column tracking_state,
    drop column current_season_number,
    drop column current_episode_number;

alter table user_episode_progress
    drop column watched;
