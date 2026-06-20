package com.app.backend.auth.dto;
import lombok.Builder;
import lombok.Data;
import java.util.Set;


@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String name;
    private String role;
    private Set<String> permisos;
}
