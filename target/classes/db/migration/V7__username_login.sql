-- Sign-in by username, email or mobile. OTP verification is removed.
DROP TABLE IF EXISTS otp_codes;
ALTER TABLE users DROP COLUMN IF EXISTS verified_at;

ALTER TABLE users ADD COLUMN username VARCHAR(30);

-- Existing accounts get a username from their email (letters/digits/._-), made unique by appending the id.
WITH base AS (
    SELECT id,
           CASE WHEN lower(regexp_replace(split_part(email, '@', 1), '[^a-zA-Z0-9._-]', '', 'g')) ~ '^[a-z][a-z0-9._-]{2,}$'
                THEN left(lower(regexp_replace(split_part(email, '@', 1), '[^a-zA-Z0-9._-]', '', 'g')), 24)
                ELSE 'user' END AS u
    FROM users),
ranked AS (SELECT id, u, row_number() OVER (PARTITION BY u ORDER BY id) AS rn FROM base)
UPDATE users SET username = CASE WHEN ranked.rn = 1 THEN ranked.u ELSE ranked.u || users.id END
  FROM ranked WHERE users.id = ranked.id;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX uq_users_username ON users (lower(username));

-- A mobile number identifies one account. Created only when existing data has no duplicates.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE phone IS NOT NULL GROUP BY phone HAVING count(*) > 1) THEN
        CREATE UNIQUE INDEX uq_users_phone ON users (phone) WHERE phone IS NOT NULL;
    END IF;
END $$;
