package com.app.backend.venta.repository;

import com.app.backend.venta.entity.PagoCxc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PagoCxcRepository extends JpaRepository<PagoCxc, Long> {
    List<PagoCxc> findByCuentaIdOrderByFechaDesc(Long idCuenta);
}