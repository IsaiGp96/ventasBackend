package com.app.backend.compra.entity;

import com.app.backend.proveedor.entity.Proveedor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orden_compra")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 20)
    private String estatus; // pendiente | recibida | cancelada

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleOrdenCompra> detalles;
    @Column(name = "es_lote", nullable = false)
    private Boolean esLote;

    @Column(name = "costo_lote", precision = 12, scale = 2)
    private BigDecimal costoLote;

    @Column(name = "piezas_lote")
    private Integer piezasLote;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal flete;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
        if (this.estatus == null)
            this.estatus = "borrador";
        if (this.flete == null)
            this.flete = BigDecimal.ZERO;
        if (this.esLote == null)
            this.esLote = false;
        if (this.flete == null)
            this.flete = BigDecimal.ZERO;

    }

}
