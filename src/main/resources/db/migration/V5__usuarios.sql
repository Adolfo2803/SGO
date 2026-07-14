CREATE TABLE usuario (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL CONSTRAINT uq_usuario_username UNIQUE,
    password_hash   VARCHAR(100)    NOT NULL,
    nombre_completo VARCHAR(150)    NOT NULL,
    rol             VARCHAR(30)     NOT NULL CHECK (rol IN ('SECRETARIA', 'DIRECTOR', 'CONSULTA')),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ     NOT NULL DEFAULT now()
);
