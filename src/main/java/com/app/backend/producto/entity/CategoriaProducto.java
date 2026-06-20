package com.app.backend.producto.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "categoria_producto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre")
    private CategoriaProducto padre;

    @Column(nullable = false)
    private Short nivel;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false)
    private Short orden;

    @OneToMany(mappedBy = "padre", fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<CategoriaProducto> hijos;
}