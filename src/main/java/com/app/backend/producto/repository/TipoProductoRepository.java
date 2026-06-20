package com.app.backend.producto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.backend.producto.entity.TipoProducto;

import java.util.List;

@Repository
public interface TipoProductoRepository extends JpaRepository<TipoProducto, Integer>{
    List<TipoProducto> findByActivoTrue();
}
