-- ============================================================
-- DAW Store — Migración V1: Schema completo
-- Flyway: V1__schema_inicial.sql
-- Refleja exactamente la BD actual en producción
-- ============================================================

-- ─── SEGURIDAD ───────────────────────────────────────────────
CREATE TABLE token_revocado (
    id               BIGSERIAL    PRIMARY KEY,
    token_hash       VARCHAR(64)  NOT NULL UNIQUE,
    email            VARCHAR(150) NOT NULL,
    fecha_revocacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP    NOT NULL
);

CREATE TABLE rate_limit (
    id             BIGSERIAL    PRIMARY KEY,
    ip             VARCHAR(45)  NOT NULL,
    endpoint       VARCHAR(100) NOT NULL,
    contador       INTEGER      NOT NULL DEFAULT 1,
    ventana_inicio TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT rate_limit_ip_endpoint_key UNIQUE (ip, endpoint)
);

CREATE TABLE idempotencia (
    id               BIGSERIAL    PRIMARY KEY,
    idempotency_key  VARCHAR(64)  NOT NULL UNIQUE,
    endpoint         VARCHAR(100) NOT NULL,
    response_status  INTEGER      NOT NULL,
    response_body    TEXT,
    fecha_creacion   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP    NOT NULL
);

-- ─── USUARIO ─────────────────────────────────────────────────
CREATE TABLE usuario (
    id             SERIAL       PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    pwd            TEXT         NOT NULL,
    rol            VARCHAR(20)  NOT NULL
        CONSTRAINT usuario_rol_check CHECK (rol IN ('ADMIN','EMPLOYEE','SUPERVISOR')),
    activo         BOOLEAN      NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_permiso (
    id_usuario INTEGER     NOT NULL REFERENCES usuario(id),
    permiso    VARCHAR(50) NOT NULL,
    CONSTRAINT usuario_permiso_pkey PRIMARY KEY (id_usuario, permiso)
);

-- ─── TIPO PRODUCTO ───────────────────────────────────────────
CREATE TABLE tipo_producto (
    id     SERIAL      PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    activo BOOLEAN     NOT NULL DEFAULT true
);

-- ─── CATEGORÍA PRODUCTO (3 niveles) ─────────────────────────
CREATE TABLE categoria_producto (
    id       SERIAL       PRIMARY KEY,
    nombre   VARCHAR(100) NOT NULL,
    slug     VARCHAR(100) NOT NULL UNIQUE,
    id_padre INTEGER      REFERENCES categoria_producto(id),
    nivel    SMALLINT     NOT NULL DEFAULT 1,
    activo   BOOLEAN      NOT NULL DEFAULT true,
    orden    SMALLINT     NOT NULL DEFAULT 0
);

-- ─── PROVEEDOR ───────────────────────────────────────────────
CREATE TABLE proveedor (
    id             SERIAL      PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    telefono       VARCHAR(50),
    correo         VARCHAR(150),
    direccion      TEXT,
    rfc            VARCHAR(20),
    prefijo        VARCHAR(10),
    clasificacion  VARCHAR(50)  DEFAULT 'otro'
        CONSTRAINT proveedor_clasificacion_check
            CHECK (clasificacion IN ('nacional','internacional','otro'))
);

-- ─── CLIENTE ─────────────────────────────────────────────────
CREATE TABLE cliente (
    id             SERIAL       PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    telefono       VARCHAR(50),
    correo         VARCHAR(150),
    direccion      TEXT,
    rfc            VARCHAR(20),
    activo         BOOLEAN      NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo           VARCHAR(20)  NOT NULL DEFAULT 'general'
        CONSTRAINT cliente_tipo_check CHECK (tipo IN ('general','mayoreo')),
    limite_credito NUMERIC(12,2) NOT NULL DEFAULT 0
);

-- ─── PRODUCTO ────────────────────────────────────────────────
CREATE TABLE producto (
    id               SERIAL       PRIMARY KEY,
    nombre           VARCHAR(150) NOT NULL,
    descripcion      TEXT,
    id_tipo_producto INTEGER      NOT NULL REFERENCES tipo_producto(id),
    id_proveedor     INTEGER      REFERENCES proveedor(id),
    precio_compra    NUMERIC(10,2),
    precio_venta     NUMERIC(10,2) NOT NULL DEFAULT 0
        CONSTRAINT producto_precio_venta_check CHECK (precio_venta >= 0),
    imagen_url       VARCHAR(255),
    activo           BOOLEAN      NOT NULL DEFAULT true,
    fecha_creacion   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_categoria     INTEGER      REFERENCES categoria_producto(id)
);

-- ─── PRODUCTO VARIANTE ───────────────────────────────────────
CREATE TABLE producto_variante (
    id             SERIAL       PRIMARY KEY,
    id_producto    INTEGER      NOT NULL REFERENCES producto(id),
    sku            VARCHAR(80)  NOT NULL UNIQUE,
    codigo_barras  VARCHAR(120) UNIQUE,
    imagen_url     VARCHAR(255),
    activo         BOOLEAN      NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    precio_compra  NUMERIC(10,2),
    precio_venta   NUMERIC(10,2),
    pct_margen     NUMERIC(5,2) NOT NULL DEFAULT 30.00,
    pct_comision   NUMERIC(5,2) NOT NULL DEFAULT 0.00
);

-- ─── VARIANTE ATRIBUTO ───────────────────────────────────────
CREATE TABLE variante_atributo (
    id          BIGSERIAL    PRIMARY KEY,
    id_variante INTEGER      NOT NULL REFERENCES producto_variante(id),
    nombre      VARCHAR(50)  NOT NULL,
    valor       VARCHAR(100) NOT NULL
);

-- ─── STOCK VARIANTE ──────────────────────────────────────────
CREATE TABLE stock_variante (
    id_variante          INTEGER   PRIMARY KEY REFERENCES producto_variante(id),
    cantidad             INTEGER   NOT NULL DEFAULT 0
        CONSTRAINT stock_variante_cantidad_check CHECK (cantidad >= 0),
    fecha_actualizacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── ORDEN COMPRA ────────────────────────────────────────────
CREATE TABLE orden_compra (
    id           SERIAL       PRIMARY KEY,
    id_proveedor INTEGER      NOT NULL REFERENCES proveedor(id),
    fecha        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estatus      VARCHAR(20)  NOT NULL,
    flete        NUMERIC(12,2) NOT NULL DEFAULT 0,
    es_lote      BOOLEAN      NOT NULL DEFAULT false,
    costo_lote   NUMERIC(12,2),
    piezas_lote  INTEGER
);

-- ─── DETALLE ORDEN COMPRA ────────────────────────────────────
CREATE TABLE detalle_orden_compra (
    id              SERIAL       PRIMARY KEY,
    id_orden_compra INTEGER      NOT NULL REFERENCES orden_compra(id),
    id_variante     INTEGER      NOT NULL REFERENCES producto_variante(id),
    cantidad        INTEGER      NOT NULL
        CONSTRAINT detalle_orden_compra_cantidad_check CHECK (cantidad > 0),
    costo_unitario  NUMERIC(10,2) NOT NULL
        CONSTRAINT detalle_orden_compra_costo_unitario_check CHECK (costo_unitario > 0),
    subtotal        NUMERIC(12,2),
    CONSTRAINT detalle_orden_compra_id_orden_compra_id_variante_key
        UNIQUE (id_orden_compra, id_variante)
);

-- ─── MOVIMIENTO INVENTARIO ───────────────────────────────────
CREATE TABLE movimiento_inventario (
    id                          BIGSERIAL   PRIMARY KEY,
    id_variante                 INTEGER     NOT NULL REFERENCES producto_variante(id),
    cantidad_delta              INTEGER     NOT NULL,
    tipo                        VARCHAR(20) NOT NULL,
    id_detalle_orden_compra     INTEGER     REFERENCES detalle_orden_compra(id),
    id_detalle_venta            INTEGER,    -- FK se agrega después de crear detalle_venta
    id_detalle_carga_inventario INTEGER,
    fecha                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario                  INTEGER     NOT NULL REFERENCES usuario(id),
    nota                        TEXT
);

-- ─── VENTA ───────────────────────────────────────────────────
CREATE TABLE venta (
    id          SERIAL       PRIMARY KEY,
    id_cliente  INTEGER      REFERENCES cliente(id),
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total       NUMERIC(12,2) NOT NULL
        CONSTRAINT venta_total_check CHECK (total >= 0),
    impuestos   NUMERIC(12,2) NOT NULL DEFAULT 0
        CONSTRAINT venta_impuestos_check CHECK (impuestos >= 0),
    estatus     VARCHAR(20)  NOT NULL
        CONSTRAINT venta_estatus_check CHECK (estatus IN ('completada','cancelada')),
    id_usuario  INTEGER      NOT NULL REFERENCES usuario(id),
    metodo_pago VARCHAR(20)  NOT NULL DEFAULT 'efectivo'
        CONSTRAINT venta_metodo_pago_check
            CHECK (metodo_pago IN ('efectivo','transferencia','tarjeta','credito')),
    descuento   NUMERIC(5,2) NOT NULL DEFAULT 0,
    notas       TEXT
);

-- ─── DETALLE VENTA ───────────────────────────────────────────
CREATE TABLE detalle_venta (
    id              SERIAL       PRIMARY KEY,
    id_venta        INTEGER      NOT NULL REFERENCES venta(id),
    id_variante     INTEGER      NOT NULL REFERENCES producto_variante(id),
    cantidad        INTEGER      NOT NULL
        CONSTRAINT detalle_venta_cantidad_check CHECK (cantidad > 0),
    precio_unitario NUMERIC(10,2) NOT NULL
        CONSTRAINT detalle_venta_precio_unitario_check CHECK (precio_unitario >= 0),
    subtotal        NUMERIC(12,2),
    pct_comision    NUMERIC(5,2) NOT NULL DEFAULT 0,
    monto_comision  NUMERIC(10,2) NOT NULL DEFAULT 0,
    precio_base     NUMERIC(10,2)
);

-- FK tardía de movimiento_inventario a detalle_venta
ALTER TABLE movimiento_inventario
    ADD CONSTRAINT movimiento_inventario_id_detalle_venta_fkey
    FOREIGN KEY (id_detalle_venta) REFERENCES detalle_venta(id);

-- ─── COMISIÓN VENTA ──────────────────────────────────────────
CREATE TABLE comision_venta (
    id             BIGSERIAL    PRIMARY KEY,
    id_venta       INTEGER      NOT NULL UNIQUE REFERENCES venta(id),
    id_usuario     INTEGER      NOT NULL REFERENCES usuario(id),
    periodo        VARCHAR(7)   NOT NULL,
    monto_comision NUMERIC(12,2) NOT NULL DEFAULT 0,
    estatus        VARCHAR(20)  NOT NULL DEFAULT 'pendiente'
        CONSTRAINT comision_venta_estatus_check CHECK (estatus IN ('pendiente','pagada')),
    fecha_pago     TIMESTAMP,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── CUENTA POR COBRAR ───────────────────────────────────────
CREATE TABLE cuenta_por_cobrar (
    id                BIGSERIAL    PRIMARY KEY,
    id_venta          BIGINT       NOT NULL UNIQUE REFERENCES venta(id),
    id_cliente        INTEGER      NOT NULL REFERENCES cliente(id),
    monto_total       NUMERIC(12,2) NOT NULL,
    monto_pagado      NUMERIC(12,2) NOT NULL DEFAULT 0,
    saldo             NUMERIC(12,2),
    estatus           VARCHAR(20)  NOT NULL DEFAULT 'pendiente'
        CONSTRAINT cuenta_por_cobrar_estatus_check
            CHECK (estatus IN ('pendiente','parcial','pagada')),
    fecha_vencimiento DATE,
    fecha_creacion    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── PAGO CXC ────────────────────────────────────────────────
CREATE TABLE pago_cxc (
    id          BIGSERIAL    PRIMARY KEY,
    id_cuenta   BIGINT       NOT NULL REFERENCES cuenta_por_cobrar(id),
    monto       NUMERIC(12,2) NOT NULL,
    metodo_pago VARCHAR(20)  NOT NULL,
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notas       TEXT
);

-- ─── CATEGORÍA GASTO ─────────────────────────────────────────
CREATE TABLE categoria_gasto (
    id       INTEGER      PRIMARY KEY,  -- IDs fijos, no secuencia
    id_padre INTEGER      REFERENCES categoria_gasto(id),
    nombre   VARCHAR(150) NOT NULL,
    nivel    INTEGER      NOT NULL
        CONSTRAINT categoria_gasto_nivel_check CHECK (nivel IN (1,2,3)),
    tipo     VARCHAR(20)  NOT NULL DEFAULT 'variable'
        CONSTRAINT categoria_gasto_tipo_check CHECK (tipo IN ('fijo','variable')),
    activo   BOOLEAN      NOT NULL DEFAULT true
);

-- Secuencia para futuros inserts dinámicos
CREATE SEQUENCE categoria_gasto_id_seq START WITH 400;

-- ─── GASTO ───────────────────────────────────────────────────
CREATE TABLE gasto (
    id                BIGSERIAL    PRIMARY KEY,
    id_categoria      INTEGER      NOT NULL REFERENCES categoria_gasto(id),
    id_usuario        INTEGER      NOT NULL REFERENCES usuario(id),
    fecha             DATE         NOT NULL,
    monto             NUMERIC(12,2) NOT NULL
        CONSTRAINT gasto_monto_check CHECK (monto > 0),
    descripcion       TEXT,
    comprobante_url   VARCHAR(255),
    fecha_creacion    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_proveedor      INTEGER      REFERENCES proveedor(id),
    pagado_por_usuario INTEGER     REFERENCES usuario(id),
    estatus_reembolso VARCHAR(20)
        CONSTRAINT gasto_estatus_reembolso_check
            CHECK (estatus_reembolso IN ('pendiente','pagado')),
    fecha_reembolso   DATE
);

-- ─── ÍNDICES ─────────────────────────────────────────────────
CREATE INDEX idx_categoria_gasto_padre        ON categoria_gasto (id_padre);
CREATE INDEX idx_token_revocado_hash          ON token_revocado (token_hash);
CREATE INDEX idx_token_revocado_expiracion    ON token_revocado (fecha_expiracion);
CREATE INDEX idx_rate_limit_ip_endpoint       ON rate_limit (ip, endpoint);
CREATE INDEX idx_idempotencia_key             ON idempotencia (idempotency_key);
CREATE INDEX idx_idempotencia_expiracion      ON idempotencia (fecha_expiracion);
CREATE INDEX idx_usuario_email                ON usuario (email);
CREATE INDEX idx_producto_tipo_activo         ON producto (id_tipo_producto, activo);
CREATE INDEX idx_producto_proveedor           ON producto (id_proveedor);
CREATE INDEX idx_producto_variante_producto   ON producto_variante (id_producto);
CREATE INDEX idx_variante_atributo_variante   ON variante_atributo (id_variante);
CREATE INDEX idx_variante_atributo_nombre     ON variante_atributo (nombre, valor);
CREATE INDEX idx_stock_variante_cantidad      ON stock_variante (cantidad);
CREATE INDEX idx_detalle_compra_orden         ON detalle_orden_compra (id_orden_compra);
CREATE INDEX idx_detalle_compra_variante      ON detalle_orden_compra (id_variante);
CREATE INDEX idx_movimiento_variante_fecha    ON movimiento_inventario (id_variante, fecha DESC);
CREATE INDEX idx_movimiento_tipo_fecha        ON movimiento_inventario (tipo, fecha DESC);
CREATE UNIQUE INDEX uq_movimiento_detalle_compra
    ON movimiento_inventario (id_detalle_orden_compra)
    WHERE id_detalle_orden_compra IS NOT NULL;
CREATE UNIQUE INDEX uq_movimiento_detalle_venta
    ON movimiento_inventario (id_detalle_venta)
    WHERE id_detalle_venta IS NOT NULL;
CREATE UNIQUE INDEX uq_movimiento_detalle_carga
    ON movimiento_inventario (id_detalle_carga_inventario)
    WHERE id_detalle_carga_inventario IS NOT NULL;
CREATE INDEX idx_detalle_venta_venta          ON detalle_venta (id_venta);
CREATE INDEX idx_detalle_venta_variante       ON detalle_venta (id_variante);
CREATE INDEX idx_venta_usuario                ON venta (id_usuario);
CREATE INDEX idx_venta_estatus                ON venta (estatus);
CREATE INDEX idx_venta_cliente                ON venta (id_cliente);
CREATE INDEX idx_comision_usuario             ON comision_venta (id_usuario);
CREATE INDEX idx_comision_periodo             ON comision_venta (periodo);
CREATE INDEX idx_comision_estatus             ON comision_venta (estatus);
CREATE INDEX idx_orden_compra_estatus         ON orden_compra (estatus);
CREATE INDEX idx_orden_compra_proveedor       ON orden_compra (id_proveedor);
CREATE INDEX idx_gasto_usuario                ON gasto (id_usuario);
CREATE INDEX idx_gasto_fecha                  ON gasto (fecha DESC);
CREATE INDEX idx_gasto_categoria              ON gasto (id_categoria);
CREATE INDEX idx_gasto_reembolso              ON gasto (estatus_reembolso)
    WHERE estatus_reembolso IS NOT NULL;

-- ─── FUNCIONES ───────────────────────────────────────────────

CREATE OR REPLACE FUNCTION fn_crear_stock_variante()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO stock_variante (id_variante, cantidad)
    VALUES (NEW.id, 0)
    ON CONFLICT (id_variante) DO NOTHING;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_validar_movimiento_inventario()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_id_variante INT;
    v_cantidad    INT;
BEGIN
    IF NEW.id_detalle_orden_compra IS NOT NULL THEN
        SELECT id_variante, cantidad
        INTO v_id_variante, v_cantidad
        FROM detalle_orden_compra
        WHERE id = NEW.id_detalle_orden_compra;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'No existe el detalle de orden de compra %',
                NEW.id_detalle_orden_compra;
        END IF;

        IF NEW.tipo <> 'ENTRADA'
           OR NEW.id_variante <> v_id_variante
           OR NEW.cantidad_delta <> v_cantidad THEN
            RAISE EXCEPTION 'Movimiento de compra inválido';
        END IF;
    END IF;

    IF NEW.id_detalle_venta IS NOT NULL THEN
        SELECT id_variante, cantidad
        INTO v_id_variante, v_cantidad
        FROM detalle_venta
        WHERE id = NEW.id_detalle_venta;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'No existe el detalle de venta %', NEW.id_detalle_venta;
        END IF;

        IF NEW.tipo <> 'SALIDA'
           OR NEW.id_variante <> v_id_variante
           OR NEW.cantidad_delta <> (v_cantidad * -1) THEN
            RAISE EXCEPTION 'Movimiento de venta inválido';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_aplicar_movimiento_stock()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    nueva_cantidad INT;
BEGIN
    INSERT INTO stock_variante (id_variante, cantidad)
    VALUES (NEW.id_variante, 0)
    ON CONFLICT (id_variante) DO NOTHING;

    UPDATE stock_variante
    SET cantidad = cantidad + NEW.cantidad_delta,
        fecha_actualizacion = CURRENT_TIMESTAMP
    WHERE id_variante = NEW.id_variante
      AND cantidad + NEW.cantidad_delta >= 0
    RETURNING cantidad INTO nueva_cantidad;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Stock insuficiente para la variante %. Movimiento solicitado: %',
            NEW.id_variante, NEW.cantidad_delta;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_movimiento_inmutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION
        'Los movimientos de inventario no se pueden modificar ni eliminar. '
        'Usa un movimiento de ajuste.';
END;
$$;

CREATE OR REPLACE FUNCTION fn_descontar_stock_venta()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO movimiento_inventario (
        id_variante, cantidad_delta, tipo,
        id_detalle_venta, id_usuario, nota, fecha
    )
    VALUES (
        NEW.id_variante,
        -NEW.cantidad,
        'SALIDA',
        NEW.id,
        (SELECT id_usuario FROM venta WHERE id = NEW.id_venta),
        'Venta #' || NEW.id_venta,
        NOW()
    );
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fn_bloquear_detalle_con_movimiento()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    existe_movimiento BOOLEAN;
BEGIN
    IF TG_TABLE_NAME = 'detalle_orden_compra' THEN
        SELECT EXISTS (
            SELECT 1 FROM movimiento_inventario
            WHERE id_detalle_orden_compra = OLD.id
        ) INTO existe_movimiento;
    ELSIF TG_TABLE_NAME = 'detalle_venta' THEN
        SELECT EXISTS (
            SELECT 1 FROM movimiento_inventario
            WHERE id_detalle_venta = OLD.id
        ) INTO existe_movimiento;
    END IF;

    IF existe_movimiento THEN
        RAISE EXCEPTION
            'No se puede modificar o eliminar un detalle que ya generó '
            'movimiento de inventario.';
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;

-- ─── TRIGGERS ────────────────────────────────────────────────

CREATE TRIGGER trg_crear_stock_variante
    AFTER INSERT ON producto_variante
    FOR EACH ROW EXECUTE FUNCTION fn_crear_stock_variante();

CREATE TRIGGER trg_validar_movimiento_inventario
    BEFORE INSERT ON movimiento_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_validar_movimiento_inventario();

CREATE TRIGGER trg_aplicar_movimiento_stock
    AFTER INSERT ON movimiento_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_aplicar_movimiento_stock();

CREATE TRIGGER trg_movimiento_no_update_delete
    BEFORE UPDATE OR DELETE ON movimiento_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_movimiento_inmutable();

CREATE TRIGGER trg_descontar_stock_venta
    AFTER INSERT ON detalle_venta
    FOR EACH ROW EXECUTE FUNCTION fn_descontar_stock_venta();

CREATE TRIGGER trg_bloquear_detalle_venta_con_movimiento
    BEFORE UPDATE OR DELETE ON detalle_venta
    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_detalle_con_movimiento();

CREATE TRIGGER trg_bloquear_detalle_compra_con_movimiento
    BEFORE UPDATE OR DELETE ON detalle_orden_compra
    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_detalle_con_movimiento();