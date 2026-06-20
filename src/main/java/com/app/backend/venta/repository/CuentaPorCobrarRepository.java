package com.app.backend.venta.repository;

import com.app.backend.venta.entity.CuentaPorCobrar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {

    @Query("SELECT c FROM CuentaPorCobrar c WHERE c.estatus IN ('pendiente','parcial') " +
            "ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorCobrar> findPendientes();

    List<CuentaPorCobrar> findByClienteIdOrderByFechaCreacionDesc(Integer idCliente);
}