-- Encore Tickets seed users.
-- Passwords are Argon2id-hashed (PHC string format), matching what
-- Spring Security's Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8() produces.
--
-- alice / BoxOffice!2025      -> role ADMIN (venue manager)
-- bob   / FrontRow2025       -> role USER  (regular customer, used in the redirect/remember-me tests)
-- carol / BackstagePass!2025  -> role USER  (regular customer, dedicated to the lockout test so it
--                                             doesn't interfere with bob's other test scenarios)

INSERT INTO users (username, password, enabled, role, failed_attempts, account_locked) VALUES
('alice', '$argon2id$v=19$m=19456,t=2,p=1$UUMN9FCMsV2V1DjIMSVGaw$qrTmoMUzt5c8WKBtOvBMv0T1UFmuWGRfLkXV5qhiOlY', true, 'ADMIN', 0, false),
('bob',   '$argon2id$v=19$m=19456,t=2,p=1$UImxS4SiyoPm0wodPezuMg$ySLsE9NQFQFLdjTjljPD0Qaf0r9Gi4BSrnf0YKJbYl8', true, 'USER', 0, false),
('carol', '$argon2id$v=19$m=19456,t=2,p=1$NkRquG569z5jEMTftxMoUQ$rc8hW3nOL0aLK135t2vIwvOBDABKROITfDOb/79eCI0', true, 'USER', 0, false);
