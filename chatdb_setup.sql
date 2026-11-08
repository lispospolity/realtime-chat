
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
    created_at BIGINT       NOT NULL,
    CONSTRAINT uq_users_username
        UNIQUE (username) -- this is temporary to make it possible to login by your name
);

CREATE TABLE IF NOT EXISTS chat.user_sessions (
    session_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL,
    sesh_key   BINARY(32)   NOT NULL,
    created_at BIGINT       NOT NULL,
    expires_at BIGINT,
    CONSTRAINT uq_user_sessions_sesh_key
        UNIQUE (sesh_key),
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id) REFERENCES chat.users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat.rooms(
    room_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    created_by VARCHAR(36) NOT NULL,
    created_at BIGINT      NOT NULL,
    CONSTRAINT fk_createdby_rooms_user
        FOREIGN KEY (created_by) REFERENCES chat.users(id)
);

CREATE TABLE IF NOT EXISTS chat.room_members(
    room_id     INT UNSIGNED NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    perms       INT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT fk_room_id_rooms
        FOREIGN KEY (room_id) REFERENCES chat.rooms(room_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_id_users
        FOREIGN KEY (user_id) REFERENCES chat.users(id)
        ON DELETE CASCADE,
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_test.users (
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    username  VARCHAR(30)  NOT NULL,
    password  VARCHAR(255) NOT NULL,
    created_at BIGINT       NOT NULL,
    CONSTRAINT uq_test_users_username
        UNIQUE (username) -- this is temporary to make it possible to login by your name
);

CREATE TABLE IF NOT EXISTS chat_test.user_sessions (
    session_id VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL,
    sesh_key   BINARY(32)   NOT NULL,
    created_at BIGINT       NOT NULL,
    expires_at BIGINT,
    CONSTRAINT uq_test_user_sessions_sesh_key
        UNIQUE (sesh_key),
    CONSTRAINT fk_test_user_sessions_user_id
        FOREIGN KEY (user_id) REFERENCES chat_test.users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_test.rooms(
    room_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    created_by VARCHAR(36) NOT NULL,
    created_at BIGINT      NOT NULL,
    CONSTRAINT fk_test_createdby_rooms_user
        FOREIGN KEY (created_by) REFERENCES chat_test.users(id)
);

CREATE TABLE IF NOT EXISTS chat_test.room_members(
    room_id     INT UNSIGNED NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    perms       INT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT fk_test_room_id_rooms
        FOREIGN KEY (room_id) REFERENCES chat_test.rooms(room_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_test_user_id_users
        FOREIGN KEY (user_id) REFERENCES chat_test.users(id)
        ON DELETE CASCADE,
    PRIMARY KEY (room_id, user_id)
);

-- 3. Insert test users into chat_test.users and chat_test.user_sessions

INSERT INTO chat_test.users (id, username, password, created_at)
VALUES
(
    'd11c5281-afe0-485d-ae55-b3588392f1c8',
    'auth_testUser',
    '$2a$10$er0XcM2CniCxEG8PEWrxKu4yv38WsgbxAh/0X9AdsAZv.ytLjgFsi',
    1791977343811
),
(
     '12345678-1234-1234-1234-b3588392f1c8',
     'room_testOwner',
     '$2a$10$er0XcM2CniCxEG8PEWrxKu4yv38WsgbxAh/0X9AdsAZv.ytLjgFsi',
     1791977343811
),
(
     '87654321-1234-1234-1234-b3588392f1c8',
     'room_testMemberAllPerms',
     '$2a$10$er0XcM2CniCxEG8PEWrxKu4yv38WsgbxAh/0X9AdsAZv.ytLjgFsi',
     1791977343811
),
(
     '87654321-4321-1234-1234-b3588392f1c8',
     'room_testMemberNoPerms',
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
),
(
    'repo_roomMember',
    'repo_testUser2',
    'qwertyuiop',
    0
);

INSERT INTO chat_test.user_sessions (session_id, user_id, sesh_key, created_at, expires_at)
VALUES
(
    'repo_existingSessionId',
    'repo_testId',
    UNHEX("6D970874D0DB767A7058798973F22CF6589601EDAB57996312F2EF7B56E5584D"),
    1791977343811,
    0
),
(
    'repo_neverExpires',
    'repo_testId',
    UNHEX("6D970874D0DB767A7058798973F22CF6589601EDAB57996312F2EF7B56E12345"),
    1791977343811,
    NULL
),
(
    'auth_deleteAll',
    'd11c5281-afe0-485d-ae55-b3588392f1c8',
    UNHEX("322F7DB21CBF1B6F634DC9AAA6A16CEB38FF220E2D9AE9986F45446F1CA936AB"),
    1791977343811,
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
