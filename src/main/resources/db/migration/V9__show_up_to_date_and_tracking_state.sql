alter table user_media_status
    modify column status enum ('NONE','TO_WATCH','WATCHING','UP_TO_DATE','WATCHED') not null;

alter table user_show_progress
    add column tracking_state enum ('TO_WATCH','WATCHING') null;
