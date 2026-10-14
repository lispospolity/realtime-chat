
-- 1. Create databases

CREATE DATABASE IF NOT EXISTS chat
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS chat_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create users table and sessions table in both databases

CREATE TABLE IF NOT EXISTS chat.users (
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    username  VARCHAR(30)  NOT NULL,
    password  VARCHAR(255) NOT NULL,
    createdat BIGINT       NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS chat_test.users (
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    username  VARCHAR(30)  NOT NULL,
    password  VARCHAR(255) NOT NULL,
    createdat BIGINT       NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS chat.user_sessions (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    sesh_key   BINARY(32)   NOT NULL,
    created_at BIGINT      NOT NULL,
    expires_at BIGINT
);

CREATE TABLE IF NOT EXISTS chat_test.user_sessions (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    sesh_key   BINARY(32)   NOT NULL,
    created_at BIGINT      NOT NULL,
    expires_at BIGINT
);


-- 3. Insert test users into chat_test.users

INSERT INTO chat_test.users (id, username, password, createdat)
VALUES
(
    'd11c5281-afe0-485d-ae55-b3588392f1c8',
    'auth_testUser',
    '$2a$10$er0XcM2CniCxEG8PEWrxKu4yv38WsgbxAh/0X9AdsAZv.ytLjgFsi',
    1791977343811
),
(
    'repo_deleteId',
    'repo_deleteMe',
    'testhash_!@#$',
    0
),
(
    'repo_testId',
    'repo_testUser',
    'qwertyuiop',
    0
);


-- 4. Create application database accounts

CREATE USER IF NOT EXISTS 'chatapp'@'localhost'
    IDENTIFIED BY 'ChangeMe1234';

CREATE USER IF NOT EXISTS 'chattestapp'@'localhost'
    IDENTIFIED BY 'ChangeMe1234';


-- 5. Grant access to the appropriate database

GRANT SELECT, INSERT, UPDATE, DELETE
    ON chat.* TO 'chatapp'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE
    ON chat_test.* TO 'chattestapp'@'localhost';


-- 6. Verify

SHOW TABLES FROM chat;
SHOW TABLES FROM chat_test;

SELECT * FROM chat_test.users;

SHOW GRANTS FOR 'chatapp'@'localhost';
SHOW GRANTS FOR 'chattestapp'@'localhost';
