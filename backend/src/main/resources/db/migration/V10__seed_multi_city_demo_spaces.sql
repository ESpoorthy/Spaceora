WITH demo(name, description, type, city, address, hourly_rate, daily_rate, capacity, image_url) AS (
    VALUES
        (
            'HITEC City Collaboration Studio',
            'Flexible coworking studio near HITEC City with high-speed Wi-Fi and quiet focus areas.',
            'COWORKING', 'Hyderabad',
            'Mindspace Road, HITEC City, Hyderabad, Telangana 500081',
            450.00, 3200.00, 12, '/images/space-04.png'
        ),
        (
            'Jubilee Hills Meeting Suite',
            'Private meeting suite in Jubilee Hills with a presentation display and comfortable seating.',
            'MEETING_ROOM', 'Hyderabad',
            'Road No. 36, Jubilee Hills, Hyderabad, Telangana 500033',
            850.00, 6000.00, 8, '/images/space-13.jpeg'
        ),
        (
            'Indiranagar Daylight Workspace',
            'Light-filled coworking space on 100 Feet Road with shared desks and fast Wi-Fi.',
            'COWORKING', 'Bangalore',
            '100 Feet Road, Indiranagar, Bangalore, Karnataka 560038',
            350.00, 2400.00, 12, '/images/space-02.png'
        ),
        (
            'Koramangala Team Room',
            'Bookable team room in Koramangala with a whiteboard and video-conferencing setup.',
            'MEETING_ROOM', 'Bangalore',
            '5th Block, Koramangala, Bangalore, Karnataka 560095',
            700.00, 5000.00, 10, '/images/space-05.jpeg'
        ),
        (
            'Baner Quiet Study Room',
            'Calm, private study room in Baner with individual desks and reliable Wi-Fi.',
            'STUDY_ROOM', 'Pune',
            'Baner Road, Baner, Pune, Maharashtra 411045',
            250.00, 1700.00, 4, '/images/space-09.png'
        ),
        (
            'Viman Nagar Workshop Loft',
            'Flexible workshop loft in Viman Nagar with movable tables and a projection wall.',
            'TRAINING_HALL', 'Pune',
            'Viman Nagar, Pune, Maharashtra 411014',
            950.00, 6800.00, 20, '/images/space-18.png'
        )
),
inserted_spaces AS (
    INSERT INTO spaces (
        owner_id, name, description, type, city, address,
        hourly_rate, daily_rate, capacity, is_active
    )
    SELECT
        owner.id, demo.name, demo.description, demo.type, demo.city, demo.address,
        demo.hourly_rate, demo.daily_rate, demo.capacity, TRUE
    FROM users AS owner
    CROSS JOIN demo
    WHERE owner.email = 'owner@spaceora.dev'
      AND owner.role = 'OWNER'
      AND NOT EXISTS (
          SELECT 1
          FROM spaces AS existing
          WHERE existing.owner_id = owner.id
            AND existing.name = demo.name
            AND existing.city = demo.city
            AND existing.address = demo.address
      )
    RETURNING id, name, city, address
),
inserted_schedules AS (
    INSERT INTO availability_schedules (space_id, day_of_week, open_time, close_time)
    SELECT inserted.id, day.day_of_week, TIME '08:00', TIME '20:00'
    FROM inserted_spaces AS inserted
    CROSS JOIN generate_series(0, 6) AS day(day_of_week)
    RETURNING id
)
INSERT INTO space_images (space_id, image_url, display_order)
SELECT inserted.id, demo.image_url, 0
FROM inserted_spaces AS inserted
JOIN demo
  ON demo.name = inserted.name
 AND demo.city = inserted.city
 AND demo.address = inserted.address;
