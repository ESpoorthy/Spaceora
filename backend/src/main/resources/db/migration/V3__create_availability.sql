CREATE TABLE availability_schedules (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    open_time TIME NOT NULL,
    close_time TIME NOT NULL CHECK (close_time > open_time),
    CONSTRAINT uq_availability_space_day UNIQUE (space_id, day_of_week)
);

CREATE TABLE blocked_periods (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    start_datetime TIMESTAMPTZ NOT NULL,
    end_datetime TIMESTAMPTZ NOT NULL CHECK (end_datetime > start_datetime),
    reason VARCHAR(500)
);
