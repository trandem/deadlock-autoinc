create table car_park_availability
(
    id              bigint auto_increment
        primary key,
    car_park_id     bigint                              not null,
    car_park_no     varchar(50)                         not null,
    total_lots      int                                 not null,
    available_lots  int                                 not null,
    lot_type        varchar(10)                         not null comment 'Type of parking lot (e.g., C for car, H for heavy vehicle, Y for motorcycle)',
    update_datetime timestamp                           not null comment 'Timestamp from the external API when the data was last updated',
    created_at      timestamp default CURRENT_TIMESTAMP not null,
    constraint idx_unique_car_park_lot_type
        unique (car_park_id, lot_type)
)
    comment 'Stores real-time availability information for car parks';


create index idx_car_park_availability_car_park_id
    on car_park_availability (car_park_id);