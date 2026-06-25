# Flyway — Setup DAW Store

## Estructura de archivos

```
src/main/resources/
  db/
    migration/
      V1__schema_inicial.sql     ← Tablas, funciones, triggers, índices
      V2__datos_catalogo.sql     ← Tipos, categorías producto, categorías gasto
```

## Dependencias Maven (pom.xml)

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

## application.properties

```properties
# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0

# Hibernate solo valida — Flyway maneja el DDL
spring.jpa.hibernate.ddl-auto=validate
```

## Primera vez en BD existente

Si la BD ya tiene datos, Flyway NO debe
re-aplicar las migraciones. Usar baseline:

```bash
# Marcar la BD actual como "ya en V2" sin re-ejecutar nada
./mvnw flyway:baseline -Dflyway.baselineVersion=2
```

O en properties:
```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=2
```

## Convención para futuras migraciones

```
V3__agregar_campo_cliente.sql
V4__multi_tenant_schema.sql
V5__configuracion_marca.sql
```

NUNCA modificar un archivo Vx ya aplicado.
Flyway guarda el checksum y fallará si detecta cambios.

## Notas importantes de este schema

1. categoria_gasto usa IDs fijos (no secuencia automática) —
   el id 106 es "Comisiones sobre Ventas (Personal)" y está
   hardcodeado en el servicio de comisiones.

2. movimiento_inventario tiene FK tardía a detalle_venta
   para resolver la dependencia circular.

3. Los triggers de inventario son inmutables por diseño —
   para corregir stock usar movimiento de tipo AJUSTE.