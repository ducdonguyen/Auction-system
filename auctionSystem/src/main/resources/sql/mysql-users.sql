CREATE
DATABASE IF NOT EXISTS auction_system;

USE
auction_system;

CREATE TABLE IF NOT EXISTS users
(
    id
    BIGINT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    full_name
    VARCHAR
(
    100
) NOT NULL,
    username VARCHAR
(
    50
) NOT NULL UNIQUE,
    email VARCHAR
(
    100
) NOT NULL UNIQUE,
    password_hash VARCHAR
(
    255
) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
