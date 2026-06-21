CREATE TABLE workflows (
    id UUID PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    payload TEXT,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);