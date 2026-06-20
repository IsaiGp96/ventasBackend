package com.app.backend.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.backend.user.entity.UsuarioPermiso;
import com.app.backend.user.entity.UsuarioPermisoId;

@Repository
public interface UsuarioPermisoRepository
        extends JpaRepository<UsuarioPermiso, UsuarioPermisoId> {

    List<UsuarioPermiso> findByUsuarioId(Integer idUsuario);

    void deleteByUsuarioId(Integer idUsuario);
}
