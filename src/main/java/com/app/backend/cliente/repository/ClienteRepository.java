package com.app.backend.cliente.repository;

import com.app.backend.cliente.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    List<Cliente> findByActivoTrueOrderByNombreAsc();

    List<Cliente> findByTipoAndActivoTrueOrderByNombreAsc(String tipo);

    boolean existsByCorreo(String correo);
}