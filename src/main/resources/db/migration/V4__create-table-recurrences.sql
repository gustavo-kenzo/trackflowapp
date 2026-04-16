create table recurrences (
    id bigint not null generated always as identity,
    user_id bigint not null,
    frequency varchar(20) not null  constraint chk_recurrences_frequency  check (frequency in ('DAILY','WEEKLY','MONTHLY','YEARLY')),
    start_date date default current_date,
    end_date date not null,
    installments_total int not null,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,

    primary key (id),
    constraint fk_recurrences_user
        foreign key(user_id)
        references users (id)
        ON DELETE CASCADE
);
create index idx_recurrences_user_id on recurrences (user_id);
