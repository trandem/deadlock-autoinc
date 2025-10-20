-- Create car_park_availability table to store real-time availability data
CREATE TABLE car_park_availability (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       car_park_id BIGINT NOT NULL,
                                       car_park_no VARCHAR(50) NOT NULL,
                                       total_lots INT NOT NULL,
                                       available_lots INT NOT NULL,
                                       lot_type VARCHAR(10) NOT NULL COMMENT 'Type of parking lot (e.g., C for car, H for heavy vehicle, Y for motorcycle)',
                                       update_datetime TIMESTAMP NOT NULL COMMENT 'Timestamp from the external API when the data was last updated',
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_car_park FOREIGN KEY (car_park_id)
                                           REFERENCES car_parks(id)
                                           ON DELETE CASCADE
) COMMENT='Stores real-time availability information for car parks';

-- Create indexes for faster queries
CREATE INDEX idx_car_park_availability_car_park_id ON car_park_availability(car_park_id);
CREATE INDEX idx_car_park_availability_car_park_no ON car_park_availability(car_park_no);
CREATE INDEX idx_car_park_availability_update_datetime ON car_park_availability(update_datetime);
CREATE INDEX idx_car_park_availability_available_lots ON car_park_availability(available_lots);

-- Create unique constraint to prevent duplicate entries for the same car park and lot type
CREATE UNIQUE INDEX idx_unique_car_park_lot_type ON car_park_availability(car_park_id, lot_type);
