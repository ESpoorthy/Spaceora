CREATE INDEX idx_spaces_city        ON spaces(city);
CREATE INDEX idx_spaces_type        ON spaces(type);
CREATE INDEX idx_spaces_capacity    ON spaces(capacity);
CREATE INDEX idx_spaces_hourly_rate ON spaces(hourly_rate);
CREATE INDEX idx_spaces_is_active   ON spaces(is_active);

CREATE INDEX idx_reservations_space_status ON reservations(space_id, status);
CREATE INDEX idx_reservations_customer     ON reservations(customer_id);

CREATE INDEX idx_blocked_periods_space ON blocked_periods(space_id);
