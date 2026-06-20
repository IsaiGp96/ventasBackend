package com.app.backend.producto.repository;

import com.app.backend.producto.dto.CatalogoProjection;
import com.app.backend.producto.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByActivoTrue();

    @Query("SELECT p FROM Producto p LEFT JOIN FETCH p.variantes v LEFT JOIN FETCH v.stock WHERE p.id = :id")
    java.util.Optional<Producto> findByIdWithVariantes(Integer id);

    @Query(value = """
            SELECT
                pv.id           AS idVariante,
                pv.sku          AS sku,
                p.id            AS idProducto,
                p.nombre        AS nombre,
                p.descripcion   AS descripcion,
                tp.nombre       AS tipo,
                pv.precio_venta AS precioVenta,
                COALESCE(sv.cantidad, 0) AS stock,
                COALESCE(pv.imagen_url, p.imagen_url) AS imagenUrl,
                COALESCE(
                    (SELECT json_object_agg(va.nombre, va.valor)
                     FROM variante_atributo va
                     WHERE va.id_variante = pv.id),
                    '{}'
                )::text AS atributos
            FROM producto_variante pv
            JOIN producto p ON p.id = pv.id_producto
            JOIN tipo_producto tp ON tp.id = p.id_tipo_producto
            LEFT JOIN stock_variante sv ON sv.id_variante = pv.id
            WHERE p.activo = true
              AND pv.activo = true
              AND COALESCE(sv.cantidad, 0) > 0
            ORDER BY p.nombre, pv.sku
            """, nativeQuery = true)
    List<CatalogoProjection> obtenerCatalogo();
}