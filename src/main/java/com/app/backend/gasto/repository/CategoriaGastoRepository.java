package com.app.backend.gasto.repository;

import com.app.backend.gasto.entity.CategoriaGasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaGastoRepository extends JpaRepository<CategoriaGasto, Integer> {

    // Solo nivel 3 activos para el selector del formulario
    List<CategoriaGasto> findByNivelAndActivoTrueOrderByNombre(Integer nivel);

    // Árbol completo activo
    List<CategoriaGasto> findByActivoTrueOrderByNivelAscNombreAsc();

    @Query("SELECT c FROM CategoriaGasto c " +
            "LEFT JOIN FETCH c.padre p " +
            "LEFT JOIN FETCH p.padre " +
            "WHERE c.nivel = :nivel AND c.activo = true " +
            "ORDER BY c.nombre")
    List<CategoriaGasto> findNivel3ConArbol(@Param("nivel") Integer nivel);
}
