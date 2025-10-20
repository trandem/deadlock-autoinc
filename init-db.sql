-- Initialize MySQL database for car park API
-- This script is executed automatically when the MySQL container starts for the first time

-- The database 'carpark_db' is already created by the MYSQL_DATABASE env variable
-- The user 'carpark_user' is already created by the MYSQL_USER env variable

-- Grant all privileges on the database to the user
GRANT ALL PRIVILEGES ON carpark_db.* TO 'carpark_user'@'%';
FLUSH PRIVILEGES;
