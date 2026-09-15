-- Inkwell seed data.
-- Passwords are Argon2id-hashed (PHC string format), matching what
-- Spring Security's Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8() produces.
--
-- alice / SupportDesk!2025  -> role ADMIN (Inkwell support agent)
-- bob   / MyContracts!2025  -> role USER  (freelancer, owns document 1)
-- carol / AgencyOwner!2025  -> role USER  (agency owner, owns document 2)

INSERT INTO users (username, password, enabled, role) VALUES
('alice', '$argon2id$v=19$m=19456,t=2,p=1$IzCB7h+7UQcrWeFnbRrWpg$SngtvMtAmLgVFDPPqJ+24scxGB1355bj4gtJ3vR8ArI', true, 'ADMIN'),
('bob',   '$argon2id$v=19$m=19456,t=2,p=1$TadGtSxK7x85kwAD8F5emQ$EdgVd4Bc7x0DNTnK7/waJmTgooasjzRHgFPdDsJMpro', true, 'USER'),
('carol', '$argon2id$v=19$m=19456,t=2,p=1$F4E02JxLbA/LRW3FA/nXvg$gRTftyYFMcWwMILOV0aCPEUNznFe79oGg2RQ/CiWavQ', true, 'USER');

INSERT INTO documents (id, title, owner_username) VALUES
(1, 'Freelance Web Design Agreement — Bob Chen', 'bob'),
(2, 'Logo & Brand Identity Contract — Carol''s Agency', 'carol');
