create table public.conference_attendees
(
    id uuid default gen_random_uuid() not null
        constraint conference_attendees_pk primary key,
    name varchar                        not null,
    role varchar                        not null
);

create unique index conference_attendees_name_uq
    on public.conference_attendees (name);

insert into conference_attendees (name, role) values ('User 1', 'Backend Developer');
insert into conference_attendees (name, role) values ('User 2', 'Frontend Developer');
insert into conference_attendees (name, role) values ('User 3', 'Lead Developer');
