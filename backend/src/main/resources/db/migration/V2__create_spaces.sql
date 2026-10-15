CREATE TABLE spaces (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('COWORKING', 'STUDY_ROOM', 'MEETING_ROOM', 'TRAINING_HALL')),
    city VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    hourly_rate NUMERIC(10,2) NOT NULL CHECK (hourly_rate >= 0),
    daily_rate NUMERIC(10,2) CHECK (daily_rate >= 0),
    capacity INT NOT NULL CHECK (capacity > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE space_images (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    display_order INT NOT NULL DEFAULT 0
);
