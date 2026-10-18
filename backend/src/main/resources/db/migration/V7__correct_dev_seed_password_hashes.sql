-- Correct the V6 development-account hashes without overwriting passwords
-- that may have been changed since the seed migration was applied.
UPDATE users
SET password_hash = '$2a$10$9d/NZU0765bi9gnFUbykVOx7oZF3JidFWp9uMZfa3erWCjRFo0aiW'
WHERE email = 'customer@spaceora.dev'
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

UPDATE users
SET password_hash = '$2a$10$PhfIcDf3u0q4wjeFLVMvUulL1xYcgVvOOS1UvNC93xCukuduY0YGK'
WHERE email = 'owner@spaceora.dev'
  AND password_hash = '$2a$10$TwLyBm/7FfwGZ7v1rDiMSu3VvHl2IJkiF.KrHFD/wH6JLl0T72GSC';

UPDATE users
SET password_hash = '$2a$10$EMXmnH2F3.aqBQ9cGwb5GuVriTZ3mQpVUvF9teNLrCNPb4HHqb0KO'
WHERE email = 'admin@spaceora.dev'
  AND password_hash = '$2a$10$Yk3nKL7CL3pqVnXd/fzIQO.5FRKtJ8mEkFjvA7F2jAL8BqE1fxoVK';
