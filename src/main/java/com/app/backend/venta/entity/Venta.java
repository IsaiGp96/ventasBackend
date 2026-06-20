package com.app.backend.venta.entity;

import com.app.backend.cliente.entity.Cliente;
import com.app.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "venta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impuestos;

    @Column(nullable = false, length = 20)
    private String estatus;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal descuento;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleVenta> detalles;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
        if (this.estatus == null)
            this.estatus = "completada";
        if (this.descuento == null)
            this.descuento = BigDecimal.ZERO;
        if (this.impuestos == null)
            this.impuestos = BigDecimal.ZERO;
    }
}