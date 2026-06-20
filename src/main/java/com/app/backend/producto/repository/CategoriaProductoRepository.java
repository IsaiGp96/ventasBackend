package com.app.backend.producto.repository;

import com.app.backend.producto.entity.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaProductoRepository
        extends JpaRepository<CategoriaProducto, Integer> {

    @Query("SELECT DISTINCT c FROM CategoriaProducto c " +
            "LEFT JOIN FETCH c.hijos h " +
            "WHERE c.padre IS NULL AND c.activo = true " +
            "ORDER BY c.orden ASC")
    List<CategoriaProducto> findRaicesConHijos();

    Optional<CategoriaProducto> findBySlug(String slug);

    List<CategoriaProducto> findAllByActivoTrueOrderByNivelAscOrdenAsc();
}
