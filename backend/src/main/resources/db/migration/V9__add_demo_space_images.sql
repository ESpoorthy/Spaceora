INSERT INTO space_images (space_id, image_url, display_order)
SELECT s.id, '/images/space-13.jpeg', 0
FROM spaces AS s
JOIN users AS u ON u.id = s.owner_id
WHERE u.email = 'owner@spaceora.dev'
  AND u.role = 'OWNER'
  AND s.name = 'Canary Wharf Focus Room'
  AND s.description = 'Modern meeting room in the heart of Canary Wharf with AV equipment and whiteboards.'
  AND s.type = 'MEETING_ROOM'
  AND s.city = 'London'
  AND s.address = '1 Canada Square, Canary Wharf, London E14 5AB'
  AND s.hourly_rate = 1200.00
  AND s.daily_rate = 120.00
  AND s.capacity = 10
  AND s.is_active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM space_images AS si WHERE si.space_id = s.id
  );

INSERT INTO space_images (space_id, image_url, display_order)
SELECT s.id, '/images/space-02.png', 0
FROM spaces AS s
JOIN users AS u ON u.id = s.owner_id
WHERE u.email = 'owner@spaceora.dev'
  AND u.role = 'OWNER'
  AND s.name = 'Shoreditch Hot Desk'
  AND s.description = 'Open coworking hot desk in vibrant Shoreditch, fast Wi-Fi, coffee included.'
  AND s.type = 'COWORKING'
  AND s.city = 'London'
  AND s.address = '91 Great Eastern Street, Shoreditch, London EC2A 3HZ'
  AND s.hourly_rate = 250.00
  AND s.daily_rate = 60.00
  AND s.capacity = 4
  AND s.is_active = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM space_images AS si WHERE si.space_id = s.id
  );
