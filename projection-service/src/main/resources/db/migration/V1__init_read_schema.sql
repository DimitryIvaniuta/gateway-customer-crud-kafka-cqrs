create schema if not exists read;
create table if not exists read.customers_view (
    id uuid primary key,
    name varchar(200) not null,
    email varchar(320) not null unique,
    version bigint not null
);
