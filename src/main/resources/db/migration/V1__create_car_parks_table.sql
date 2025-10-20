CREATE TABLE car_parks (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           car_park_no VARCHAR(50) NOT NULL,
                           address TEXT NOT NULL,
                           latitude DECIMAL(10,7) NOT NULL COMMENT 'Latitude in WGS84 format',
                           longitude DECIMAL(10,7) NOT NULL COMMENT 'Longitude in WGS84 format',
                           car_park_type VARCHAR(50),
                           type_of_parking_system VARCHAR(100),
                           short_term_parking VARCHAR(50),
                           parking_hours_from TIME COMMENT 'Parking start time (TIME datatype, e.g., 07:00:00)',
                           parking_hours_to TIME COMMENT 'Parking end time (TIME datatype, e.g., 22:30:00)',
                           free_parking VARCHAR(50),
                           night_parking VARCHAR(50),
                           car_park_decks INT,
                           gantry_height DECIMAL(5,2),
                           car_park_basement VARCHAR(10),
                           total_lots INT COMMENT 'Total parking lots capacity',
                           total_available_lots INT COMMENT 'Currently available parking lots',
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='Stores static information about HDB car parks in Singapore';

CREATE INDEX idx_car_parks_car_park_no ON car_parks(car_park_no);
CREATE INDEX idx_car_parks_latitude ON car_parks(latitude);
CREATE INDEX idx_car_parks_longitude ON car_parks(longitude);
