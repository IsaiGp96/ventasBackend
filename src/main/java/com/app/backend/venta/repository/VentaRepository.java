package com.app.backend.venta.repository;

import com.app.backend.venta.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    @Query("SELECT v FROM Venta v LEFT JOIN FETCH v.detalles d " +
            "LEFT JOIN FETCH d.variante WHERE v.id = :id")
    Optional<Venta> findByIdWithDetalles(Integer id);

    List<Venta> findAllByOrderByFechaDesc();

    List<Venta> findByUsuarioIdOrderByFechaDesc(Integer idUsuario);
}