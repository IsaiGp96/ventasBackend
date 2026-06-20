package com.app.backend.producto.repository;

import com.app.backend.producto.entity.Producto;
import com.app.backend.producto.entity.ProductoVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Integer> {
    List<ProductoVariante> findByProducto(Producto producto);

    Optional<ProductoVariante> findBySku(String sku);

    boolean existsBySku(String sku);
}