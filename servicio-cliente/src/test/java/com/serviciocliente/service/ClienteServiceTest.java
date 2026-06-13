package com.serviciocliente.service;

import com.serviciocliente.FeingClient.ReniecClient;
import com.serviciocliente.dto.request.CreateClienteRequest;
import com.serviciocliente.dto.response.ReniecResponse;
import com.serviciocliente.entity.Cliente;
import com.serviciocliente.exception.ConflictException;
import com.serviciocliente.exception.ResourceNotFoundException;
import com.serviciocliente.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ReniecClient reniecClient;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private ClienteService clienteService;

    private CreateClienteRequest request;
    private ReniecResponse reniecResponse;
    private Cliente clienteEsperado;

    @BeforeEach
    void setUp() {
        request = new CreateClienteRequest();
        request.setDni("12345678");
        request.setEmail("juan.perez@correo.com");
        request.setTelefono("987654321");

        reniecResponse = new ReniecResponse("Juan", "Perez", "Gomez", "12345678");

        clienteEsperado = new Cliente();
        clienteEsperado.setDni("12345678");
        clienteEsperado.setNombre("Juan");
        clienteEsperado.setApellidoPaterno("Perez");
        clienteEsperado.setApellidoMaterno("Gomez");
        clienteEsperado.setEmail("juan.perez@correo.com");
        clienteEsperado.setTelefono("987654321");
        ReflectionTestUtils.setField(clienteService, "reniecToken", "sk_14050.w5IiZP8rPe5uRDITZ6LnIPyDDBnO9hK5");
    }

    @Test
    void registrarCliente_HappyPath() {
        when(clienteRepository.existsByDni("12345678")).thenReturn(false);
        when(clienteRepository.existsByEmail("juan.perez@correo.com")).thenReturn(false);
        
        when(reniecClient.consultarDni(eq("12345678"), any(), any())).thenReturn(reniecResponse);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente resultado = clienteService.registrarCliente(request);

        assertNotNull(resultado);
        assertEquals("12345678", resultado.getDni());
        assertEquals("Juan", resultado.getNombre());
        assertEquals("Perez", resultado.getApellidoPaterno());
        assertEquals("Gomez", resultado.getApellidoMaterno());
        assertEquals("juan.perez@correo.com", resultado.getEmail());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void registrarCliente_ExcepcionDniDuplicado() {
        when(clienteRepository.existsByDni("12345678")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, 
                () -> clienteService.registrarCliente(request));

        assertEquals("Ya existe un cliente registrado con el DNI: 12345678", ex.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void registrarCliente_ExcepcionEmailDuplicado() {
        when(clienteRepository.existsByDni("12345678")).thenReturn(false);
        when(clienteRepository.existsByEmail("juan.perez@correo.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, 
                () -> clienteService.registrarCliente(request));

        assertEquals("Ya existe un cliente registrado con el email: juan.perez@correo.com", ex.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void buscarPorDni_ExcepcionNoEncontrado() {
        when(clienteRepository.findByDni("00000000")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, 
                () -> clienteService.buscarPorDni("00000000"));

        assertEquals("No se encontró cliente con el DNI: 00000000", ex.getMessage());
    }
}
