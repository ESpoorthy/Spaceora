-- Passwords are BCrypt hashes:
--   customer@spaceora.dev  ->  Customer1!
--   owner@spaceora.dev     ->  Owner1!
--   admin@spaceora.dev     ->  Admin1!
-- Generated with BCrypt cost 10 offline; safe to commit.

INSERT INTO users (email, password_hash, first_name, last_name, role) VALUES
('customer@spaceora.dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice', 'Customer', 'CUSTOMER'),
('owner@spaceora.dev',    '$2a$10$TwLyBm/7FfwGZ7v1rDiMSu3VvHl2IJkiF.KrHFD/wH6JLl0T72GSC', 'Bob', 'Owner', 'OWNER'),
('admin@spaceora.dev',    '$2a$10$Yk3nKL7CL3pqVnXd/fzIQO.5FRKtJ8mEkFjvA7F2jAL8BqE1fxoVK', 'Carol', 'Admin', 'ADMIN');

INSERT INTO spaces (owner_id, name, description, type, city, address, hourly_rate, daily_rate, capacity, is_active)
VALUES
(2, 'Canary Wharf Focus Room',
 'Modern meeting room in the heart of Canary Wharf with AV equipment and whiteboards.',
 'MEETING_ROOM', 'London', '1 Canada Square, Canary Wharf, London E14 5AB',
 15.00, 120.00, 10, TRUE),
(2, 'Shoreditch Hot Desk',
 'Open coworking hot desk in vibrant Shoreditch, fast Wi-Fi, coffee included.',
 'COWORKING', 'London', '91 Great Eastern Street, Shoreditch, London EC2A 3HZ',
 8.00, 60.00, 4, TRUE);

INSERT INTO availability_schedules (space_id, day_of_week, open_time, close_time) VALUES
(1, 0, '08:00', '20:00'), (1, 1, '08:00', '20:00'), (1, 2, '08:00', '20:00'),
(1, 3, '08:00', '20:00'), (1, 4, '08:00', '20:00'),
(2, 0, '07:00', '22:00'), (2, 1, '07:00', '22:00'), (2, 2, '07:00', '22:00'),
(2, 3, '07:00', '22:00'), (2, 4, '07:00', '22:00'), (2, 5, '09:00', '18:00');

INSERT INTO reservations (space_id, customer_id, start_datetime, end_datetime, total_amount, status, notes)
VALUES (
    1, 1,
    NOW() + INTERVAL '30 days' + TIME '10:00:00',
    NOW() + INTERVAL '30 days' + TIME '12:00:00',
    30.00, 'PENDING', 'First booking -- M0 seed data'
);
