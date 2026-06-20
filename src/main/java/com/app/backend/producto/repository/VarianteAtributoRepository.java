package com.app.backend.producto.repository;

import com.app.backend.producto.entity.ProductoVariante;
import com.app.backend.producto.entity.VarianteAtributo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VarianteAtributoRepository extends JpaRepository<VarianteAtributo, Long> {

    List<VarianteAtributo> findByVarianteId(Integer idVariante);

    // Buscar variante por producto y conjunto exacto de atributos
    @Query("""
            SELECT pv FROM ProductoVariante pv
            WHERE pv.producto.id = :idProducto
            AND (
                SELECT COUNT(va) FROM VarianteAtributo va
                WHERE va.variante = pv
                AND CONCAT(va.nombre, '=', va.valor) IN :atributosKV
            ) = :totalAtributos
            AND (
                SELECT COUNT(va2) FROM VarianteAtributo va2
                WHERE va2.variante = pv
            ) = :totalAtributos
            """)
    Optional<ProductoVariante> findByProductoAndAtributos(
            @Param("idProducto") Integer idProducto,
            @Param("atributosKV") List<String> atributosKV,
            @Param("totalAtributos") long totalAtributos);
}