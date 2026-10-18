UPDATE spaces AS s
SET hourly_rate = 1200.00
FROM users AS u
WHERE s.owner_id = u.id
  AND u.email = 'owner@spaceora.dev'
  AND u.role = 'OWNER'
  AND s.name = 'Canary Wharf Focus Room'
  AND s.description = 'Modern meeting room in the heart of Canary Wharf with AV equipment and whiteboards.'
  AND s.type = 'MEETING_ROOM'
  AND s.city = 'London'
  AND s.address = '1 Canada Square, Canary Wharf, London E14 5AB'
  AND s.hourly_rate = 15.00
  AND s.daily_rate = 120.00
  AND s.capacity = 10
  AND s.is_active = TRUE;

UPDATE spaces AS s
SET hourly_rate = 250.00
FROM users AS u
WHERE s.owner_id = u.id
  AND u.email = 'owner@spaceora.dev'
  AND u.role = 'OWNER'
  AND s.name = 'Shoreditch Hot Desk'
  AND s.description = 'Open coworking hot desk in vibrant Shoreditch, fast Wi-Fi, coffee included.'
  AND s.type = 'COWORKING'
  AND s.city = 'London'
  AND s.address = '91 Great Eastern Street, Shoreditch, London EC2A 3HZ'
  AND s.hourly_rate = 8.00
  AND s.daily_rate = 60.00
  AND s.capacity = 4
  AND s.is_active = TRUE;
