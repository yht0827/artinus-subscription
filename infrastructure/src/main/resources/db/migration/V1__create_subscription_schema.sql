create table members (
    id bigint primary key auto_increment,
    phone_number varchar(20) not null,
    subscription_status varchar(20) not null,
    version bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint uk_members_phone_number unique (phone_number)
);

create table channels (
    id bigint primary key auto_increment,
    name varchar(50) not null,
    subscribe_enabled boolean not null,
    cancel_enabled boolean not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint uk_channels_name unique (name)
);

create table subscription_histories (
    id bigint primary key auto_increment,
    member_id bigint not null,
    channel_id bigint not null,
    action_type varchar(20) not null,
    before_status varchar(20) not null,
    after_status varchar(20) not null,
    changed_at datetime(6) not null,
    constraint fk_histories_member foreign key (member_id) references members (id),
    constraint fk_histories_channel foreign key (channel_id) references channels (id)
);

create index idx_histories_member_changed_at
on subscription_histories (member_id, changed_at);

create table idempotency_keys (
    id bigint primary key auto_increment,
    phone_number varchar(20) not null,
    idempotency_key varchar(100) not null,
    request_hash varchar(128) not null,
    response_body text null,
    status_code int null,
    processing_status varchar(20) not null,
    created_at datetime(6) not null,
    completed_at datetime(6) null,
    constraint uk_idempotency_phone_key unique (phone_number, idempotency_key)
);

create index idx_idempotency_created_at
on idempotency_keys (created_at);
