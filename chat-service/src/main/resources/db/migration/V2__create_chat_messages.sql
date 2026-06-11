create table chat_message_sequences (
    room_id uuid primary key,
    next_sequence bigint not null,
    constraint fk_chat_message_sequences_room_id foreign key (room_id) references chat_rooms (id),
    constraint ck_chat_message_sequences_next_sequence check (next_sequence >= 1)
);

create table chat_messages (
    id uuid primary key,
    room_id uuid not null,
    sender_id varchar(64) not null,
    sequence bigint not null,
    type varchar(20) not null,
    content varchar(2000) not null,
    created_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_chat_messages_room_id foreign key (room_id) references chat_rooms (id),
    constraint ck_chat_messages_type check (type in ('TEXT')),
    constraint ck_chat_messages_sequence check (sequence >= 1),
    constraint ck_chat_messages_content_not_blank check (length(trim(content)) > 0)
);

create unique index uk_chat_messages_room_id_sequence on chat_messages (room_id, sequence);
create index idx_chat_messages_room_id_created_at on chat_messages (room_id, created_at);
create index idx_chat_messages_room_id_sequence on chat_messages (room_id, sequence);
create index idx_chat_messages_sender_id on chat_messages (sender_id);
