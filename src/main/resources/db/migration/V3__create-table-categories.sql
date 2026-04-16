create table categories (
    id bigint not null generated always as identity,
    user_id bigint not null,
    name varchar(150) not null,
    type varchar(20) not null  constraint chk_categories_type  check (type in ('INCOME','EXPENSE')),
    system_default boolean default false,
    active boolean default true,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,


    primary key (id),
    constraint fk_categories_user
        foreign key (user_id)
        references users (id)
        ON DELETE CASCADE,

    constraint uq_categories_user_name
        unique(user_id,name)

);