package com.app.backend.producto.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CategoriaTreeResponse {
    private Integer id;
    private String nombre;
    private String slug;
    private Short nivel;
    private List<CategoriaTreeResponse> hijos;
}