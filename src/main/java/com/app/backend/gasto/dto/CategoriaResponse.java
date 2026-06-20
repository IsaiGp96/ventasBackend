package com.app.backend.gasto.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaResponse {
    private Integer id;
    private String nombre;
    private String tipo;
    private String subcategoria; // nombre del padre nivel 2
    private String grupo; // nombre del padre nivel 1
}