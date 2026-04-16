create table accounts (
    id bigint not null generated always as identity,
    user_id bigint not null,

    name varchar(150) not null,
    type varchar(20) not null  constraint chk_account_type  check (type in ('CHECKING','SAVINGS','WALLET','CREDIT_CARD')),
    opening_balance numeric(15,2) not null default 0,
    current_balance numeric(15,2) not null default 0,
    active boolean default true,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,

    primary key (id),
    constraint fk_accounts_user
        foreign key (user_id)
        references users (id)
        ON DELETE CASCADE
);
create index idx_accounts_user_id on accounts (user_id);