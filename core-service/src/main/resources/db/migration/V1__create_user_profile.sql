CREATE TABLE user_profile (
    user_id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_user_profile_email_lower ON user_profile (LOWER(email));
