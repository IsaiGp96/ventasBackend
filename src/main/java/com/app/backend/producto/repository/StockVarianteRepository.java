package com.app.backend.producto.repository;

import com.app.backend.producto.entity.StockVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockVarianteRepository extends JpaRepository<StockVariante, Integer> {
}
