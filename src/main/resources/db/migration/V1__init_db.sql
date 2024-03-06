create table public.conference_attendees
(
    id   uuid default gen_random_uuid() not null,
    name varchar                        not null,
    reg_number integer                  not null
);

insert into conference_attendees (name, reg_number) values ('User 1', 1);
insert into conference_attendees (name, reg_number) values ('User 2', 2);
insert into conference_attendees (name, reg_number) values ('User 3', 3);
