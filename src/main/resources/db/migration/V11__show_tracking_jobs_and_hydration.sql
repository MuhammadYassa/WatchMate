create table show_tracking_job (
    id bigint not null auto_increment,
    user_id bigint null,
    media_id bigint not null,
    job_type enum ('HYDRATE_SHOW_CATALOG','MARK_SHOW_WATCHED','MARK_SHOW_UP_TO_DATE','SET_SHOW_PROGRESS') not null,
    status enum ('PENDING','RUNNING','COMPLETED','FAILED') not null,
    requested_status enum ('TO_WATCH','WATCHING','UP_TO_DATE','WATCHED','NONE') null,
    target_season_number int null,
    target_episode_number int null,
    total_seasons int null,
    completed_seasons int not null default 0,
    failed_seasons_json json null,
    error_code varchar(100) null,
    error_message text null,
    attempt_count int not null default 0,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    started_at datetime(6) null,
    completed_at datetime(6) null,
    primary key (id),
    constraint fk_show_tracking_job_user foreign key (user_id) references users (id),
    constraint fk_show_tracking_job_media foreign key (media_id) references media (id)
);

create index idx_show_tracking_job_status on show_tracking_job (status);
create index idx_show_tracking_job_user_id on show_tracking_job (user_id);
create index idx_show_tracking_job_media_id on show_tracking_job (media_id);
create index idx_show_tracking_job_job_type on show_tracking_job (job_type);
create index idx_show_tracking_job_created_at on show_tracking_job (created_at);
create index idx_show_tracking_job_active_lookup on show_tracking_job (status, user_id, media_id, job_type, created_at);
