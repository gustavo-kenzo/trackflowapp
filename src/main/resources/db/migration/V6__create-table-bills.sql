create table bills (
    id bigint not null generated always as identity,
    transaction_id bigint not null,
    user_id bigint not null,
    type varchar(20) not null  constraint chk_bills_type  check (type in ('INCOME','EXPENSE')),
    status varchar(20) not null  constraint chk_bills_status check (status in ('PENDING','SETTLED')),
    due_date date default current_date,
    paid_date date,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp,

    primary key (id),
    constraint fk_bills_account
        foreign key(transaction_id)
        references transactions(id)
        ON DELETE CASCADE,
    constraint fk_bills_user
        foreign key(user_id)
        references users (id)
        ON DELETE CASCADE
);
create index idx_bills_status_due on bills (user_id, status, due_date);