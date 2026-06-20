package com.app.backend.producto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "producto_variante")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(unique = true, nullable = false, length = 80)
    private String sku;

    @Column(name = "codigo_barras", unique = true, length = 120)
    private String codigoBarras;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", updatable = false)
    private java.time.LocalDateTime fechaCreacion;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @OneToMany(mappedBy = "variante", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<VarianteAtributo> atributos = new ArrayList<>();

    @OneToOne(mappedBy = "variante", cascade = CascadeType.ALL)
    private StockVariante stock;

    // Helper — obtener atributos como mapa
    public Map<String, String> getAtributosMap() {
        return atributos.stream()
                .collect(Collectors.toMap(
                        VarianteAtributo::getNombre,
                        VarianteAtributo::getValor,
                        (a, b) -> b));
    }

    // Helper — obtener valor de un atributo
    public String getAtributo(String nombre) {
        return atributos.stream()
                .filter(a -> a.getNombre().equals(nombre))
                .map(VarianteAtributo::getValor)
                .findFirst()
                .orElse(null);
    }

    @Column(name = "precio_compra", precision = 10, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "pct_margen", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctMargen;

    @Column(name = "pct_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctComision;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = java.time.LocalDateTime.now();
        if (this.activo == null)
            this.activo = true;
        if (this.pctMargen == null)
            this.pctMargen = new BigDecimal("30.00");
        if (this.pctComision == null)
            this.pctComision = BigDecimal.ZERO;
    }
}