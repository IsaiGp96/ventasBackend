package com.app.backend.compra.service;

import com.app.backend.compra.dto.CalculoPrecioRequest;
import com.app.backend.compra.dto.CalculoPrecioResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CalculoPrecioService {

        public CalculoPrecioResponse calcular(CalculoPrecioRequest req) {
                BigDecimal costo = req.getCostoUnitario() != null
                                ? req.getCostoUnitario()
                                : BigDecimal.ZERO;

                // Flete prorrateado por pieza
                BigDecimal fletePorPieza = BigDecimal.ZERO;
                if (req.getFlete() != null && req.getFlete().compareTo(BigDecimal.ZERO) > 0
                                && req.getTotalMercancia() != null
                                && req.getTotalMercancia().compareTo(BigDecimal.ZERO) > 0
                                && req.getCantidad() != null && req.getCantidad() > 0) {

                        BigDecimal valorLinea = costo
                                        .multiply(BigDecimal.valueOf(req.getCantidad()));
                        BigDecimal proporcion = valorLinea
                                        .divide(req.getTotalMercancia(), 6, RoundingMode.HALF_UP);
                        BigDecimal fleteLinea = req.getFlete().multiply(proporcion);
                        fletePorPieza = fleteLinea
                                        .divide(BigDecimal.valueOf(req.getCantidad()), 4, RoundingMode.HALF_UP);
                }

                BigDecimal costoReal = costo.add(fletePorPieza)
                                .setScale(4, RoundingMode.HALF_UP);

                // Factores multiplicadores
                BigDecimal pctComision = req.getPctComision() != null
                                ? req.getPctComision()
                                : BigDecimal.ZERO;
                BigDecimal pctMargen = req.getPctMargen() != null
                                ? req.getPctMargen()
                                : BigDecimal.ZERO;

                boolean exento = Boolean.TRUE.equals(req.getExentoIva());
                BigDecimal pctIva = exento ? BigDecimal.ZERO
                                : (req.getPctIva() != null ? req.getPctIva() : new BigDecimal("16"));

                BigDecimal factorComision = BigDecimal.ONE.add(
                                pctComision.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                BigDecimal factorIva = BigDecimal.ONE.add(
                                pctIva.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));

                // Paso 1: aplicar margen
                BigDecimal precioConMargen;
                if (pctMargen.compareTo(new BigDecimal("100")) >= 0) {
                        throw new IllegalArgumentException("El margen no puede ser 100% o mayor");
                }
                BigDecimal divisorMargen = BigDecimal.ONE.subtract(
                                pctMargen.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                precioConMargen = costoReal.multiply(factorComision).multiply(factorIva)
                                .divide(divisorMargen, 4, RoundingMode.HALF_UP);

                // Paso 2: aplicar comisión
                BigDecimal precioSugerido;
                if (pctComision.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal divisorComision = BigDecimal.ONE.subtract(
                                        pctComision.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                        precioSugerido = precioConMargen
                                        .divide(divisorComision, 2, RoundingMode.HALF_UP);
                } else {
                        precioSugerido = precioConMargen.setScale(2, RoundingMode.HALF_UP);
                }

                BigDecimal margenPesos = precioSugerido.subtract(costoReal)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal pctMargenReal = precioSugerido.compareTo(BigDecimal.ZERO) > 0
                                ? margenPesos.divide(precioSugerido, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                                .setScale(1, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                return CalculoPrecioResponse.builder()
                                .costoUnitario(costo.setScale(2, RoundingMode.HALF_UP))
                                .fletePorPieza(fletePorPieza.setScale(2, RoundingMode.HALF_UP))
                                .costoReal(costoReal.setScale(2, RoundingMode.HALF_UP))
                                .factorComision(factorComision.setScale(4, RoundingMode.HALF_UP))
                                .factorIva(factorIva.setScale(4, RoundingMode.HALF_UP))
                                .precioSugerido(precioSugerido)
                                .margenPesos(margenPesos)
                                .pctMargenReal(pctMargenReal)
                                .build();
        }
}