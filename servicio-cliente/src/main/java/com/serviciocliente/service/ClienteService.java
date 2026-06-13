package com.serviciocliente.service;

import com.serviciocliente.FeingClient.ReniecClient;
import com.serviciocliente.dto.request.CreateClienteRequest;
import com.serviciocliente.dto.response.ReniecResponse;
import com.serviciocliente.entity.Cliente;
import com.serviciocliente.repository.ClienteRepository;
import com.serviciocliente.exception.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviciocliente.exception.ResourceNotFoundException;
import org.modelmapper.ModelMapper;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ReniecClient reniecClient;
    private final ModelMapper modelMapper;

    @Value("${reniec.api.token:sk_14050.w5IiZP8rPe5uRDITZ6LnIPyDDBnO9hK5}")
    private String reniecToken;

    public ClienteService(ClienteRepository clienteRepository, ReniecClient reniecClient, ModelMapper modelMapper) {
        this.clienteRepository = clienteRepository;
        this.reniecClient = reniecClient;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public Cliente registrarCliente(CreateClienteRequest request) {
        if (clienteRepository.existsByDni(request.getDni())) {
            throw new ConflictException("Ya existe un cliente registrado con el DNI: " + request.getDni());
        }
        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un cliente registrado con el email: " + request.getEmail());
        }

        // Conversión entidad DTO mediante ModelMapper
        Cliente nuevoCliente = modelMapper.map(request, Cliente.class);

        //.trim() limpia el salto de línea y Mozilla simula un navegador
        ReniecResponse datosReniec = reniecClient.consultarDni(
                request.getDni(),
                buildAuthorizationHeader(),
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        );

        nuevoCliente.setNombre(datosReniec.nombres());
        nuevoCliente.setApellidoPaterno(datosReniec.apellidoPaterno());
        nuevoCliente.setApellidoMaterno(datosReniec.apellidoMaterno());

        return clienteRepository.save(nuevoCliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorDni(String dni) {
        return clienteRepository.findByDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cliente con el DNI: " + dni));
    }

    @Transactional(readOnly = true)
    public java.util.List<Cliente> buscarPorTexto(String texto) {
        return clienteRepository.buscarPorTexto(texto);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cliente con el email: " + email));
    }

    private String buildAuthorizationHeader() {
        String token = reniecToken.trim();
        return token.regionMatches(true, 0, "Bearer ", 0, 7) ? token : "Bearer " + token;
    }
}
