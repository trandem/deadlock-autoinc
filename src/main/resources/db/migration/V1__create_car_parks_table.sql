create table car_parks
(
    id                     bigint auto_increment
        primary key,
    car_park_no            varchar(50)                         not null,
    address                text                                not null,
    latitude               decimal(10, 7)                      not null comment 'Latitude in WGS84 format',
    longitude              decimal(10, 7)                      not null comment 'Longitude in WGS84 format',
    car_park_type          varchar(50)                         null,
    type_of_parking_system varchar(100)                        null,
    short_term_parking     varchar(50)                         null,
    parking_hours_from     time                                null comment 'Parking start time (TIME datatype, e.g., 07:00:00)',
    parking_hours_to       time                                null comment 'Parking end time (TIME datatype, e.g., 22:30:00)',
    free_parking           varchar(50)                         null,
    night_parking          varchar(50)                         null,
    car_park_decks         int                                 null,
    gantry_height          decimal(5, 2)                       null,
    car_park_basement      varchar(10)                         null,
    total_lots             int                                 null comment 'Total parking lots capacity',
    total_available_lots   int                                 null comment 'Currently available parking lots',
    created_at             timestamp default CURRENT_TIMESTAMP not null,
    updated_at             timestamp default CURRENT_TIMESTAMP not null,
    constraint car_parks_pk
        unique (car_park_no)
)
    comment 'Stores static information about HDB car parks in Singapore';

create index idx_car_parks_car_park_no
    on car_parks (car_park_no);

