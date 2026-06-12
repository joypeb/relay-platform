create table chat_room_read_states (
    room_id uuid not null,
    member_id varchar(64) not null,
    last_read_sequence bigint not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (room_id, member_id),
    constraint fk_chat_room_read_states_room_id foreign key (room_id) references chat_rooms (id),
    constraint ck_chat_room_read_states_last_read_sequence check (last_read_sequence >= 0)
);

create index idx_chat_room_read_states_member_id on chat_room_read_states (member_id);
