create schema if not exists write;
create table if not exists write.customers (
                                               id uuid primary key,
                                               name varchar(200) not null,
    email varchar(320) not null unique,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
    );
create table if not exists write.outbox (
                                            id bigserial primary key,
                                            aggregate_type varchar(64) not null,
    aggregate_id uuid not null,
    event_type varchar(64) not null,
    version bigint not null,
    payload jsonb not null,
    occurred_at timestamptz not null default now(),
    published boolean not null default false,
    event_id uuid not null unique
    );
