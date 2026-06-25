-- ============================================================
-- DAW Store — Migración V2: Datos de catálogo
-- Flyway: V2__datos_catalogo.sql
-- Datos estáticos necesarios para el funcionamiento del sistema
-- ============================================================

-- ─── TIPOS DE PRODUCTO ───────────────────────────────────────
INSERT INTO tipo_producto (codigo, nombre, activo) VALUES
    ('ROPA', 'Ropa',        true),
    ('CALZ', 'Calzado',     true),
    ('ACC',  'Accesorios',  true);

-- ─── CATEGORÍAS DE PRODUCTO ──────────────────────────────────
-- Nivel 1 — Categoría principal
INSERT INTO categoria_producto (id, nombre, slug, nivel, orden, activo) VALUES
    (1, 'Ropa',       'ropa',       1, 1, true),
    (2, 'Calzado',    'calzado',    1, 2, true),
    (3, 'Accesorios', 'accesorios', 1, 3, true);

-- Nivel 2 — Género
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (4,  'Hombre', 'ropa-hombre',      1, 2, 1, true),
    (5,  'Mujer',  'ropa-mujer',       1, 2, 2, true),
    (6,  'Unisex', 'ropa-unisex',      1, 2, 3, true),
    (7,  'Hombre', 'calzado-hombre',   2, 2, 1, true),
    (8,  'Mujer',  'calzado-mujer',    2, 2, 2, true),
    (9,  'Unisex', 'calzado-unisex',   2, 2, 3, true),
    (10, 'Hombre', 'acc-hombre',       3, 2, 1, true),
    (11, 'Mujer',  'acc-mujer',        3, 2, 2, true),
    (12, 'Unisex', 'acc-unisex',       3, 2, 3, true);

-- Nivel 3 — Ropa Hombre
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (13, 'Camisas',       'ropa-hombre-camisas',    4, 3, 1, true),
    (14, 'Playeras',      'ropa-hombre-playeras',   4, 3, 2, true),
    (15, 'Pantalones',    'ropa-hombre-pantalones', 4, 3, 3, true),
    (16, 'Shorts',        'ropa-hombre-shorts',     4, 3, 4, true),
    (17, 'Ropa Interior', 'ropa-hombre-interior',   4, 3, 5, true),
    (18, 'Deportiva',     'ropa-hombre-deportiva',  4, 3, 6, true);

-- Nivel 3 — Ropa Mujer
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (19, 'Blusas',        'ropa-mujer-blusas',      5, 3, 1, true),
    (20, 'Playeras',      'ropa-mujer-playeras',    5, 3, 2, true),
    (21, 'Pantalones',    'ropa-mujer-pantalones',  5, 3, 3, true),
    (22, 'Faldas',        'ropa-mujer-faldas',      5, 3, 4, true),
    (23, 'Ropa Interior', 'ropa-mujer-interior',    5, 3, 5, true),
    (24, 'Deportiva',     'ropa-mujer-deportiva',   5, 3, 6, true);

-- Nivel 3 — Ropa Unisex
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (25, 'Calcetines', 'ropa-unisex-calcetines', 6, 3, 1, true),
    (26, 'Sudaderas',  'ropa-unisex-sudaderas',  6, 3, 2, true),
    (27, 'Gorras',     'ropa-unisex-gorras',     6, 3, 3, true);

-- Nivel 3 — Calzado Hombre
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (28, 'Casual',    'calzado-hombre-casual',    7, 3, 1, true),
    (29, 'Deportivo', 'calzado-hombre-deportivo', 7, 3, 2, true),
    (30, 'Trabajo',   'calzado-hombre-trabajo',   7, 3, 3, true),
    (31, 'Formal',    'calzado-hombre-formal',    7, 3, 4, true);

-- Nivel 3 — Calzado Mujer
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (32, 'Casual',    'calzado-mujer-casual',     8, 3, 1, true),
    (33, 'Deportivo', 'calzado-mujer-deportivo',  8, 3, 2, true),
    (34, 'Trabajo',   'calzado-mujer-trabajo',    8, 3, 3, true),
    (35, 'Tacones',   'calzado-mujer-tacones',    8, 3, 4, true);

-- Nivel 3 — Calzado Unisex
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (36, 'Sandalias', 'calzado-unisex-sandalias', 9, 3, 1, true),
    (37, 'Pantuflas', 'calzado-unisex-pantuflas', 9, 3, 2, true);

-- Nivel 3 — Accesorios
INSERT INTO categoria_producto (id, nombre, slug, id_padre, nivel, orden, activo) VALUES
    (38, 'Cinturones', 'acc-hombre-cinturones', 10, 3, 1, true),
    (39, 'Carteras',   'acc-hombre-carteras',   10, 3, 2, true),
    (40, 'Bolsas',     'acc-mujer-bolsas',      11, 3, 1, true),
    (41, 'Joyería',    'acc-mujer-joyeria',     11, 3, 2, true),
    (42, 'Relojes',    'acc-unisex-relojes',    12, 3, 1, true),
    (43, 'Mochilas',   'acc-unisex-mochilas',   12, 3, 2, true);

-- Resetear secuencia
SELECT setval('categoria_producto_id_seq', 43);

-- ─── CATEGORÍAS DE GASTO ─────────────────────────────────────
-- Nivel 1
INSERT INTO categoria_gasto (id, nombre, nivel, tipo, activo) VALUES
    (1,  'Gastos Operativos de Ventas',        1, 'variable', true),
    (2,  'Gastos Operativos de Administración',1, 'variable', true),
    (3,  'Gastos Financieros',                 1, 'fijo',     true);

-- Nivel 2
INSERT INTO categoria_gasto (id, nombre, nivel, tipo, id_padre, activo) VALUES
    (10, 'Ocupación y Local',                  2, 'fijo',     1, true),
    (11, 'Marketing y Publicidad',             2, 'variable', 1, true),
    (12, 'Logística y Empaque',                2, 'variable', 1, true),
    (13, 'Operación Comercial',                2, 'variable', 1, true),
    (20, 'Servicios Públicos y Conectividad',  2, 'fijo',     2, true),
    (21, 'Infraestructura y Tecnología',       2, 'fijo',     2, true),
    (22, 'Servicios Profesionales',            2, 'variable', 2, true),
    (23, 'Mantenimiento y Oficina',            2, 'variable', 2, true),
    (30, 'Costos Bancarios',                   2, 'fijo',     3, true);

-- Nivel 3
INSERT INTO categoria_gasto (id, nombre, nivel, tipo, id_padre, activo) VALUES
    (100, 'Arrendamiento de Local Comercial',          3, 'fijo',     10, true),
    (101, 'Publicidad Digital (Meta, Google, TikTok)', 3, 'variable', 11, true),
    (102, 'Publicidad Impresa y Material POP',         3, 'variable', 11, true),
    (103, 'Envíos y Fletes a Clientes',                3, 'variable', 12, true),
    (104, 'Material de Empaque',                       3, 'variable', 12, true),
    (105, 'Comisiones de Pasarelas de Pago y POS',     3, 'variable', 13, true),
    (106, 'Comisiones sobre Ventas (Personal)',         3, 'variable', 13, true),
    (200, 'Energía Eléctrica',                         3, 'fijo',     20, true),
    (201, 'Servicio de Agua Comercial',                3, 'fijo',     20, true),
    (202, 'Internet y Telefonía',                      3, 'fijo',     20, true),
    (203, 'Servidores y Hosting',                      3, 'fijo',     21, true),
    (204, 'Dominios y Certificados SSL',               3, 'fijo',     21, true),
    (205, 'Licencias de Software y APIs',              3, 'fijo',     21, true),
    (206, 'Honorarios de Contabilidad y Asesoría',     3, 'variable', 22, true),
    (207, 'Honorarios de Servicios Legales',           3, 'variable', 22, true),
    (208, 'Artículos de Papelería y Oficina',          3, 'variable', 23, true),
    (209, 'Insumos de Limpieza e Higiene',             3, 'variable', 23, true),
    (210, 'Mantenimiento de Mobiliario',               3, 'variable', 23, true),
    (300, 'Comisiones Bancarias por Manejo de Cuenta', 3, 'fijo',     30, true),
    (301, 'Intereses por Préstamos y Créditos',        3, 'variable', 30, true);

-- Actualizar secuencia al máximo usado
SELECT setval('categoria_gasto_id_seq', 400);