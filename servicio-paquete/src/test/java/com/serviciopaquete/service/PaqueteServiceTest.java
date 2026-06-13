package com.serviciopaquete.service;

import com.serviciopaquete.dto.request.CreatePaqueteRequest;
import com.serviciopaquete.dto.request.UpdatePaqueteRequest;
import com.serviciopaquete.dto.response.ClienteDto;
import com.serviciopaquete.dto.response.PaqueteResponse;
import com.serviciopaquete.dto.response.HistorialEstadoResponse;
import com.serviciopaquete.entity.Paquete;
import com.serviciopaquete.exception.ConflictException;
import com.serviciopaquete.exception.ResourceNotFoundException;
import com.serviciopaquete.repository.CategoriaRepository;
import com.serviciopaquete.repository.PaqueteRepository;
import com.serviciopaquete.config.TarifaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaqueteServiceTest {

    @Mock
    private PaqueteRepository paqueteRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ClienteLookupService clienteLookupService;

    @Mock
    private TarifaProperties tarifaProperties;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private PaqueteService paqueteService;

    private CreatePaqueteRequest request;
    private Paquete paqueteEsperado;
    private ClienteDto clienteDtoRemitente;
    private ClienteDto clienteDtoDestinatario;

    @BeforeEach
    void setUp() {
        request = new CreatePaqueteRequest();
        request.setDniRemitente("12345678");
        request.setDniDestinatario("87654321");
        request.setPesoKg(2.0);
        request.setValorDeclarado(new BigDecimal("100.00"));
        request.setSucursalOrigen("Lima");
        request.setSucursalDestino("Arequipa");

        paqueteEsperado = new Paquete();
        paqueteEsperado.setId(UUID.randomUUID());
        paqueteEsperado.setDniRemitente("12345678");
        paqueteEsperado.setDniDestinatario("87654321");
        paqueteEsperado.setPesoKg(2.0);
        paqueteEsperado.setValorDeclarado(new BigDecimal("100.00"));
        paqueteEsperado.setSucursalOrigen("Lima");
        paqueteEsperado.setSucursalDestino("Arequipa");
        paqueteEsperado.setEstadoActual("REGISTRADO");
        paqueteEsperado.setCodigoRastreo("RC-A1B2C3D4");
        paqueteEsperado.setHistorial(new ArrayList<>());

        clienteDtoRemitente = new ClienteDto();
        clienteDtoRemitente.setDni("12345678");
        clienteDtoRemitente.setNombre("Juan");
        clienteDtoRemitente.setApellidoPaterno("Perez");

        clienteDtoDestinatario = new ClienteDto();
        clienteDtoDestinatario.setDni("87654321");
        clienteDtoDestinatario.setNombre("Maria");
        clienteDtoDestinatario.setApellidoPaterno("Gomez");
    }

    private void mockTarifas() {
        lenient().when(tarifaProperties.getPrecioBase()).thenReturn(new BigDecimal("10.00"));
        lenient().when(tarifaProperties.getFactorPeso()).thenReturn(new BigDecimal("5.00"));
    }

    @Test
    void crearPaquete_HappyPath() {
        mockTarifas();
        // Mocking para obtención de clientes existentes en creación
        doNothing().when(clienteLookupService).verificarClienteExistente("12345678");
        doNothing().when(clienteLookupService).verificarClienteExistente("87654321");

        // Mocking para verificación de unicidad de codigoRastreo
        when(paqueteRepository.findByCodigoRastreo(any(String.class))).thenReturn(Optional.empty());

        when(clienteLookupService.obtenerClienteDto("12345678")).thenReturn(clienteDtoRemitente);
        when(clienteLookupService.obtenerClienteDto("87654321")).thenReturn(clienteDtoDestinatario);

        // Mocking para guardado en bd
        when(paqueteRepository.save(any(Paquete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaqueteResponse resultado = paqueteService.crearPaquete(request, "operador@correo.com");

        assertNotNull(resultado);
        assertEquals("12345678", resultado.getDniRemitente());
        assertEquals("87654321", resultado.getDniDestinatario());
        assertEquals("REGISTRADO", resultado.getEstadoActual());
        assertNotNull(resultado.getCodigoRastreo());
        assertTrue(resultado.getCodigoRastreo().startsWith("RC-"));

        // Tarifa esperada: 10 + 2*5 + 100*0.02 = 22
        assertEquals(0, new BigDecimal("22.00").compareTo(resultado.getTarifa()));
        verify(paqueteRepository, times(1)).save(any(Paquete.class));
    }

    @Test
    void crearPaquete_ResilienciaFallback() {
        mockTarifas();
        doNothing().when(clienteLookupService).verificarClienteExistente("12345678");
        doNothing().when(clienteLookupService).verificarClienteExistente("87654321");

        // Mocking para verificación de unicidad de codigoRastreo
        when(paqueteRepository.findByCodigoRastreo(any(String.class))).thenReturn(Optional.empty());

        ClienteDto fallbackRemitente = new ClienteDto();
        fallbackRemitente.setDni("12345678");
        fallbackRemitente.setNombre("No Disponible");
        fallbackRemitente.setApellidoPaterno("(Servicio Caído)");

        ClienteDto fallbackDestinatario = new ClienteDto();
        fallbackDestinatario.setDni("87654321");
        fallbackDestinatario.setNombre("No Disponible");
        fallbackDestinatario.setApellidoPaterno("(Servicio Caído)");

        when(clienteLookupService.obtenerClienteDto("12345678")).thenReturn(fallbackRemitente);
        when(clienteLookupService.obtenerClienteDto("87654321")).thenReturn(fallbackDestinatario);

        when(paqueteRepository.save(any(Paquete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaqueteResponse resultado = paqueteService.crearPaquete(request, "operador@correo.com");

        assertNotNull(resultado);
        assertEquals("12345678", resultado.getDniRemitente());
        assertEquals("87654321", resultado.getDniDestinatario());
        assertEquals("No Disponible", resultado.getRemitente().getNombre());
        assertEquals("No Disponible", resultado.getDestinatario().getNombre());
        verify(paqueteRepository, times(1)).save(any(Paquete.class));
    }

    @Test
    void rastrearPaquete_HappyPath() {
        when(clienteLookupService.obtenerClienteDto("12345678")).thenReturn(clienteDtoRemitente);
        when(clienteLookupService.obtenerClienteDto("87654321")).thenReturn(clienteDtoDestinatario);

        when(paqueteRepository.findByCodigoRastreo("RC-A1B2C3D4")).thenReturn(Optional.of(paqueteEsperado));

        PaqueteResponse resultado = paqueteService.rastrearPaquete("RC-A1B2C3D4", null, null);

        assertNotNull(resultado);
        assertEquals("RC-A1B2C3D4", resultado.getCodigoRastreo());
        assertEquals("Juan", resultado.getRemitente().getNombre());
    }

    @Test
    void rastrearPaquete_ExcepcionNoEncontrado() {
        when(paqueteRepository.findByCodigoRastreo("RC-INVAL")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, 
                () -> paqueteService.rastrearPaquete("RC-INVAL", null, null));

        assertTrue(ex.getMessage().contains("Paquete no encontrado"));
    }

    @Test
    void actualizarEstado_HappyPath() {
        when(clienteLookupService.obtenerClienteDto("12345678")).thenReturn(clienteDtoRemitente);
        when(clienteLookupService.obtenerClienteDto("87654321")).thenReturn(clienteDtoDestinatario);

        when(paqueteRepository.findById(paqueteEsperado.getId())).thenReturn(Optional.of(paqueteEsperado));
        when(paqueteRepository.save(any(Paquete.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaqueteResponse resultado = paqueteService.actualizarEstado(paqueteEsperado.getId(), "EN_TRANSITO", "operador@correo.com");

        assertNotNull(resultado);
        assertEquals("EN_TRANSITO", resultado.getEstadoActual());
        assertEquals(1, paqueteEsperado.getHistorial().size());
        assertEquals("REGISTRADO", paqueteEsperado.getHistorial().get(0).getEstadoAnterior());
        assertEquals("EN_TRANSITO", paqueteEsperado.getHistorial().get(0).getEstadoNuevo());
    }

    @Test
    void actualizarEstado_TransicionInvalida() {
        when(paqueteRepository.findById(paqueteEsperado.getId())).thenReturn(Optional.of(paqueteEsperado));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> paqueteService.actualizarEstado(paqueteEsperado.getId(), "ENTREGADO", "operador@correo.com"));

        assertTrue(ex.getMessage().contains("Transición de estado inválida"));
        verify(paqueteRepository, never()).save(any(Paquete.class));
    }

    @Test
    void buscarYFiltrarPaquetes_SinResultados_RetornaListaVacia() {
        when(paqueteRepository.findBySucursalAndEstado("Lima", "ENTREGADO"))
                .thenReturn(Collections.emptyList());

        java.util.List<PaqueteResponse> resultado =
                paqueteService.buscarYFiltrarPaquetes(null, "Lima", "ENTREGADO", null, null);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}
