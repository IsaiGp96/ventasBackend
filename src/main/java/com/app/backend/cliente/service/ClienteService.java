package com.app.backend.cliente.service;

import com.app.backend.cliente.dto.ClienteRequest;
import com.app.backend.cliente.dto.ClienteResponse;
import com.app.backend.cliente.entity.Cliente;
import com.app.backend.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findByActivoTrueOrderByNombreAsc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarPorTipo(String tipo) {
        return clienteRepository.findByTipoAndActivoTrueOrderByNombreAsc(tipo)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Integer id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        if (request.getCorreo() != null && !request.getCorreo().isBlank()
                && clienteRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException(
                    "Ya existe un cliente con ese correo");
        }

        return toResponse(clienteRepository.save(
                Cliente.builder()
                        .nombre(request.getNombre())
                        .tipo(request.getTipo())
                        .telefono(request.getTelefono())
                        .correo(request.getCorreo())
                        .direccion(request.getDireccion())
                        .rfc(request.getRfc())
                        .limiteCredito(request.getLimiteCredito() != null
                                ? request.getLimiteCredito()
                                : BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public ClienteResponse actualizar(Integer id, ClienteRequest request) {
        Cliente cliente = buscarPorId(id);
        cliente.setNombre(request.getNombre());
        cliente.setTipo(request.getTipo());
        cliente.setTelefono(request.getTelefono());
        cliente.setCorreo(request.getCorreo());
        cliente.setDireccion(request.getDireccion());
        cliente.setRfc(request.getRfc());
        cliente.setLimiteCredito(request.getLimiteCredito() != null
                ? request.getLimiteCredito()
                : BigDecimal.ZERO);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void desactivar(Integer id) {
        Cliente cliente = buscarPorId(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente buscarPorId(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    public ClienteResponse toResponse(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .tipo(c.getTipo())
                .telefono(c.getTelefono())
                .correo(c.getCorreo())
                .direccion(c.getDireccion())
                .rfc(c.getRfc())
                .limiteCredito(c.getLimiteCredito())
                .activo(c.getActivo())
                .fechaCreacion(c.getFechaCreacion())
                .tieneCredito(c.getLimiteCredito() != null
                        && c.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0)
                .build();
    }
}
