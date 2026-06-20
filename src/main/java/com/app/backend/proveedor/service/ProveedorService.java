package com.app.backend.proveedor.service;

import com.app.backend.proveedor.dto.ProveedorRequest;
import com.app.backend.proveedor.dto.ProveedorResponse;
import com.app.backend.proveedor.entity.Proveedor;
import com.app.backend.proveedor.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Integer id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        Proveedor proveedor = Proveedor.builder()
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .direccion(request.getDireccion())
                .rfc(request.getRfc())
                .build();
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(Integer id, ProveedorRequest request) {
        Proveedor proveedor = buscarPorId(id);
        proveedor.setNombre(request.getNombre());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setCorreo(request.getCorreo());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setRfc(request.getRfc());
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public void eliminar(Integer id) {
        Proveedor proveedor = buscarPorId(id);
        try {
            proveedorRepository.delete(proveedor);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el proveedor porque tiene órdenes de compra asociadas");
        }
    }

    private Proveedor buscarPorId(Integer id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    }

    private ProveedorResponse toResponse(Proveedor p) {
        return ProveedorResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .telefono(p.getTelefono())
                .correo(p.getCorreo())
                .direccion(p.getDireccion())
                .rfc(p.getRfc())
                .build();
    }
}