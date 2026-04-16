create table users (
    id bigint not null generated always as identity,
    name varchar(150) not null,
    email varchar(150) not null unique,
    password varchar(255) not null,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,

    primary key(id)
);