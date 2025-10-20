-- Create car_park_not_found table to track car parks from API that are not in our database
CREATE TABLE car_park_not_found (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     car_park_no VARCHAR(50) NOT NULL COMMENT 'Car park number from the external API',
                                     first_seen_at TIMESTAMP NOT NULL COMMENT 'Timestamp when this car park was first detected as missing',
                                     last_seen_at TIMESTAMP NOT NULL COMMENT 'Timestamp when this car park was last detected as missing',
                                     occurrence_count INT NOT NULL DEFAULT 1 COMMENT 'Number of times this car park was found in API but not in database',
                                     CONSTRAINT unique_car_park_no UNIQUE (car_park_no)
) COMMENT='Tracks car parks that appear in the API response but are not found in our car_parks table';

-- Create index for faster lookups
CREATE INDEX idx_car_park_not_found_car_park_no ON car_park_not_found(car_park_no);
CREATE INDEX idx_car_park_not_found_last_seen_at ON car_park_not_found(last_seen_at);