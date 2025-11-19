CREATE DATABASE IF NOT EXISTS string_menu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'app'@'localhost' IDENTIFIED BY 'app123';
GRANT ALL PRIVILEGES ON string_menu.* TO 'app'@'localhost';
FLUSH PRIVILEGES;

USE string_menu;

-- Таблица для задания №3
CREATE TABLE IF NOT EXISTS int_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    original_text VARCHAR(64) NOT NULL,
    is_integer TINYINT(1) NOT NULL,
    int_value INT NULL,
    is_even TINYINT(1) NULL,
    note VARCHAR(255) NULL
);
