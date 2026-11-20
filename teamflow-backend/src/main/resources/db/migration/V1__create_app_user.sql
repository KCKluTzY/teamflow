CREATE TABLE app_user (
    id            uuid         NOT NULL DEFAULT gen_random_uuid(),
    email         varchar(255) NOT NULL,
    password_hash varchar(60)  NOT NULL,
    first_name    varchar(100) NOT NULL,
    last_name     varchar(100) NOT NULL,
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_email_lower CHECK (email = lower(email))
);