alter table user_media_status
    add column updated_at datetime(6) null;

update user_media_status
set updated_at = coalesce(last_watched_at, now(6))
where updated_at is null;

alter table user_media_status
    modify column updated_at datetime(6) not null;
