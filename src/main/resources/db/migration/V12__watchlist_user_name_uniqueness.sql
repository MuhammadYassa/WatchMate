alter table watch_list
    add column normalized_name varchar(255)
        generated always as (lower(name)) stored;

create unique index uk_watchlist_user_name
    on watch_list (user_id, normalized_name);
