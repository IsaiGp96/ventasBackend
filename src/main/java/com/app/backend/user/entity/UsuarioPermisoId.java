package com.app.backend.user.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPermisoId implements java.io.Serializable {
    private Integer usuario;
    private String permiso;
}