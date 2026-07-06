create table transactions (
    id bigint not null generated always as identity,
    user_id bigint not null,
    account_id bigint not null,
    category_id bigint,
    recurrence_id bigint,
    amount numeric(15,2) not null,
    type varchar(20) not null  constraint chk_transactions_type  check (type in ('INCOME','EXPENSE')),
    status varchar(20) not null  constraint chk_transactions_status check (status in ('PENDING','SETTLED')),
    competence_date date not null,
    settlement_date date,
    description varchar(255),
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,

    primary key (id),
    constraint fk_transactions_user
        foreign key(user_id)
        references users (id)
        ON DELETE CASCADE,
    constraint fk_transactions_account
        foreign key(account_id)
        references accounts(id)
        ON DELETE CASCADE,
    constraint fk_transactions_category
        foreign key(category_id)
        references categories (id)
        ON DELETE SET NULL,
    constraint fk_transactions_recurrence
        foreign key(recurrence_id)
        references recurrences (id)
        ON DELETE SET NULL,

    constraint chk_transactions_amount
        check (amount > 0)
);
create index idx_transactions_user_id on transactions (user_id);