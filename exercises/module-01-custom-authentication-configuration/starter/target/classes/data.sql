-- Udabank seed users.
-- Passwords are Argon2id-hashed (PHC string format), matching what
-- Spring Security's Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8() produces.
--
-- alice / VaultAdmin!2025  -> role ADMIN
-- bob   / QaTester!2025    -> role USER

INSERT INTO users (username, password, enabled, role) VALUES
('alice', '$argon2id$v=19$m=19456,t=2,p=1$5Huaj0XZZrvNLXgq056zNA$NdGK4vXqwOlOGtqgM+FGhwVYTY//HERxxrYw6XmzDjI', true, 'ADMIN'),
('bob',   '$argon2id$v=19$m=19456,t=2,p=1$1KtsbcIy+FOuHBa4gsBwxg$J3bz3sBtYc8m/ow5ZlEMmyPnnzmQrkFuNm0GvdPFwMU', true, 'USER');
