package com.app.backend.gasto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.backend.gasto.entity.Gasto;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

        @Query("SELECT g FROM Gasto g WHERE g.categoria.id = :idCategoria " +
                        "ORDER BY g.fecha DESC")
        List<Gasto> findByCategoria(@Param("idCategoria") Integer idCategoria);

        @Query("SELECT g FROM Gasto g " +
                        "JOIN FETCH g.categoria c " +
                        "LEFT JOIN FETCH c.padre p " +
                        "LEFT JOIN FETCH p.padre " +
                        "ORDER BY g.fecha DESC, g.fechaCreacion DESC")
        List<Gasto> findAllByOrderByFechaDescFechaCreacionDesc();

        @Query("SELECT g FROM Gasto g " +
                        "JOIN FETCH g.categoria c " +
                        "LEFT JOIN FETCH c.padre p " +
                        "LEFT JOIN FETCH p.padre " +
                        "WHERE g.fecha BETWEEN :inicio AND :fin " +
                        "ORDER BY g.fecha DESC")
        List<Gasto> findByPeriodo(
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin);

        @Query("SELECT g FROM Gasto g WHERE g.fecha BETWEEN :inicio AND :fin AND g.usuario.id = :usuarioId ORDER BY g.fecha DESC")
        List<Gasto> findByPeriodoUsuario(
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin,
                        @Param("usuarioId") Integer usuarioId);

        @Query("SELECT g FROM Gasto g " +
                        "JOIN FETCH g.categoria c " +
                        "LEFT JOIN FETCH c.padre p " +
                        "LEFT JOIN FETCH p.padre " +
                        "WHERE g.estatusReembolso = :estatus " +
                        "ORDER BY g.fecha DESC")
        List<Gasto> findByEstatusReembolso(@Param("estatus") String estatus);

        List<Gasto> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

}