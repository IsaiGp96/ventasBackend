package com.app.backend.venta.repository;

import com.app.backend.venta.entity.ComisionVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ComisionVentaRepository extends JpaRepository<ComisionVenta, Long> {
        @Query("SELECT c FROM ComisionVenta c " +
                        "JOIN FETCH c.usuario " +
                        "JOIN FETCH c.venta " +
                        "WHERE c.estatus = :estatus " +
                        "ORDER BY c.fechaCreacion DESC")
        List<ComisionVenta> findByEstatusOrderByFechaCreacionDesc(@Param("estatus") String estatus);

        @Query("SELECT c FROM ComisionVenta c " +
                        "JOIN FETCH c.usuario " +
                        "JOIN FETCH c.venta " +
                        "WHERE c.periodo = :periodo " +
                        "ORDER BY c.fechaCreacion DESC")
        List<ComisionVenta> findByPeriodo(@Param("periodo") String periodo);

        @Query("SELECT c FROM ComisionVenta c " +
                        "JOIN FETCH c.usuario " +
                        "JOIN FETCH c.venta " +
                        "WHERE c.usuario.id = :idUsuario " +
                        "ORDER BY c.fechaCreacion DESC")
        List<ComisionVenta> findByUsuarioId(@Param("idUsuario") Integer idUsuario);

        @Query("SELECT c FROM ComisionVenta c " +
                        "JOIN FETCH c.usuario " +
                        "JOIN FETCH c.venta " +
                        "WHERE c.usuario.id = :idUsuario AND c.periodo = :periodo " +
                        "ORDER BY c.fechaCreacion DESC")
        List<ComisionVenta> findByUsuarioIdAndPeriodoOrderByFechaCreacionDesc(
                        @Param("idUsuario") Integer idUsuario,
                        @Param("periodo") String periodo);

        @Query("SELECT c FROM ComisionVenta c " +
                        "JOIN FETCH c.usuario " +
                        "JOIN FETCH c.venta " +
                        "ORDER BY c.fechaCreacion DESC")
        List<ComisionVenta> findAllByOrderByFechaCreacionDesc();

        List<ComisionVenta> findByUsuarioIdAndEstatusOrderByFechaCreacionDesc(
                        Integer idUsuario, String estatus);
}
