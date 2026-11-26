-- Table des refresh token qui permettent de rester connecter sans avoir à ressaisir son mot de passe
-- On ne stocke JAMAIS le token mais seulement son empreinte, SHA-256 fait en sorte que si la base fuite les empreintes ne permettent pas de se connecter
CREATE TABLE refresh_token (
    id         uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL,
    token_hash varchar(64) NOT NULL, -- Empreinte SHA-256 du token en hexadecimal
    family_id  uuid        NOT NULL, -- Tous les tokens issus d'une même connexion partagent la ême famille . 
    -- A chaque renouvellement, le token précédent est révoqué et un nouveau naît dans cette même famille. 
    -- Si un token déjà révoqué est présenté à nouveau (vol), on révoque toute la famille.
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash), -- Un même token ne peut pas exister plusieurs fois cela sert à le retrouver par son empreinte.
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) 
        REFERENCES app_user (id) ON DELETE CASCADE, --Supprimer un utilisateur supprime par conséquent ses tokens.
    CONSTRAINT ck_refresh_token_hash_format CHECK (token_hash ~ '^[0-9a-f]{64}'),
    CONSTRAINT ck_refresh_token_expiry CHECK (expires_at > created_at) -- Un token doit expirer après sa création.
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);
