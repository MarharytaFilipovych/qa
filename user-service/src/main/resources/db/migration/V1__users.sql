CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    beauty_name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    birth_date DATE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);