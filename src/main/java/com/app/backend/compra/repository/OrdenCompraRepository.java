package com.app.backend.compra.repository;

import com.app.backend.compra.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Integer> {

    List<OrdenCompra> findAllByOrderByFechaDesc();

    @Query("SELECT o FROM OrdenCompra o LEFT JOIN FETCH o.detalles d LEFT JOIN FETCH d.variante WHERE o.id = :id")
    Optional<OrdenCompra> findByIdWithDetalles(Integer id);

    List<OrdenCompra> findByEstatus(String estatus);

    @Query("SELECT o FROM OrdenCompra o " +
            "LEFT JOIN FETCH o.detalles d " +
            "LEFT JOIN FETCH d.variante v " +
            "LEFT JOIN FETCH v.producto " +
            "LEFT JOIN FETCH v.atributos " +
            "WHERE o.estatus = :estatus " +
            "ORDER BY o.fecha DESC")
    List<OrdenCompra> findByEstatusWithDetalles(@Param("estatus") String estatus);

}
