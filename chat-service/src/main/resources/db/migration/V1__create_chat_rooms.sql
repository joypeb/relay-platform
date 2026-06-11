create table chat_rooms (
    id uuid primary key,
    name varchar(100) not null,
    description varchar(500),
    owner_id varchar(64) not null,
    visibility varchar(20) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    version bigint not null,
    constraint ck_chat_rooms_visibility check (visibility in ('PUBLIC', 'PRIVATE')),
    constraint ck_chat_rooms_status check (status in ('ACTIVE', 'DELETED'))
);

create table chat_room_members (
    id uuid primary key,
    room_id uuid not null,
    member_id varchar(64) not null,
    role varchar(20) not null,
    joined_at timestamp with time zone not null,
    left_at timestamp with time zone,
    constraint fk_chat_room_members_room_id foreign key (room_id) references chat_rooms (id),
    constraint ck_chat_room_members_role check (role in ('OWNER', 'MEMBER'))
);

create index idx_chat_rooms_visibility_created_at on chat_rooms (visibility, created_at desc);
create index idx_chat_rooms_owner_id on chat_rooms (owner_id);
create index idx_chat_room_members_member_id on chat_room_members (member_id);
create index idx_chat_room_members_room_id on chat_room_members (room_id);
create unique index uk_chat_room_members_room_id_member_id_active
    on chat_room_members (room_id, member_id)
    where left_at is null;
