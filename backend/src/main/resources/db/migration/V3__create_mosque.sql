-- The tenant. Every piece of mosque-specific data references this table through a mandatory mosque_id.
-- The slug identifies the mosque in public URLs and in the app configuration (white-label).

CREATE TABLE mosque (
    id            UUID         NOT NULL,
    slug          VARCHAR(60)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    description   VARCHAR(4000),
    street        VARCHAR(150),
    postal_code   VARCHAR(20),
    city          VARCHAR(100),
    country_code  VARCHAR(2)   NOT NULL,
    email         VARCHAR(254),
    phone         VARCHAR(30),
    website_url   VARCHAR(500),
    time_zone     VARCHAR(50)  NOT NULL,
    active        BOOLEAN      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL,
    CONSTRAINT pk_mosque PRIMARY KEY (id),
    CONSTRAINT uk_mosque_slug UNIQUE (slug),
    CONSTRAINT ck_mosque_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_mosque_country_code CHECK (country_code ~ '^[A-Z]{2}$')
);
