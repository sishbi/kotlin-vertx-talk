create table public.test_users
(
    id   uuid default gen_random_uuid() not null,
    name varchar                        not null,
    age  integer                        not null
);

insert into test_users (name, age) values ('User 1', 11);
insert into test_users (name, age) values ('User 2', 21);
insert into test_users (name, age) values ('User 3', 33);
