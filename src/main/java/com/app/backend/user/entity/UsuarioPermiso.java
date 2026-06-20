package com.app.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario_permiso")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UsuarioPermisoId.class)
public class UsuarioPermiso {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private User usuario;

    @Id
    @Column(nullable = false, length = 50)
    private String permiso;
}
