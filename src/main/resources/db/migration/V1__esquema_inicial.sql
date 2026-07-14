-- Áreas del hospital a las que se turnan oficios
CREATE TABLE area (
    id          BIGSERIAL       PRIMARY KEY,
    nombre      VARCHAR(150)    NOT NULL,
    correo      VARCHAR(150)    NOT NULL,
    activa      BOOLEAN         NOT NULL DEFAULT TRUE
);

-- Oficios de entrada y salida
CREATE TABLE oficio (
    id              BIGSERIAL       PRIMARY KEY,
    folio           VARCHAR(50)     NOT NULL CONSTRAINT uq_oficio_folio UNIQUE,
    direccion       VARCHAR(10)     NOT NULL CHECK (direccion IN ('ENTRADA', 'SALIDA')),
    fecha           DATE            NOT NULL,
    remitente       VARCHAR(255)    NOT NULL,
    destinatario    VARCHAR(255)    NOT NULL,
    asunto          VARCHAR(500)    NOT NULL,
    estado          VARCHAR(15)     NOT NULL CHECK (estado IN ('RECIBIDO', 'TURNADO', 'ARCHIVADO')),
    ruta_archivo    VARCHAR(500),
    creado_por      VARCHAR(100)    NOT NULL,
    creado_en       TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_oficio_fecha ON oficio (fecha);
CREATE INDEX idx_oficio_estado ON oficio (estado);
CREATE INDEX idx_oficio_direccion ON oficio (direccion);

-- Registro de turnado de un oficio a un área
CREATE TABLE turnado (
    id              BIGSERIAL       PRIMARY KEY,
    oficio_id       BIGINT          NOT NULL REFERENCES oficio(id),
    area_id         BIGINT          NOT NULL REFERENCES area(id),
    estado          VARCHAR(15)     NOT NULL CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    intentos        INT             NOT NULL DEFAULT 0,
    enviado_en      TIMESTAMPTZ,
    ultimo_error    VARCHAR(1000)
);
