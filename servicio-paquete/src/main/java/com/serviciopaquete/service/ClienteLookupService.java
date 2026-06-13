package com.serviciopaquete.service;

import com.serviciopaquete.client.ClienteClient;
import com.serviciopaquete.dto.ApiResponse;
import com.serviciopaquete.dto.response.ClienteDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClienteLookupService {

    private final ClienteClient clienteClient;

    public ClienteLookupService(ClienteClient clienteClient) {
        this.clienteClient = clienteClient;
    }

    // Circuit Breaker & Retry para validación de cliente
    //Resilience4j intercepta la excepción vía AOP y activa el fallback
    @CircuitBreaker(name = "clienteCB", fallbackMethod = "fallbackVerificarCliente")
    @Retry(name = "clienteRetry")
    public void verificarClienteExistente(String dni) {
        ApiResponse<ClienteDto> apiResp = clienteClient.obtenerClientePorDni(dni);
        if (apiResp == null || !apiResp.isSuccess()) {
            throw new IllegalStateException("Respuesta inválida de servicio-cliente al verificar DNI: " + dni);
        }
    }

    // Fallback degradado: Permite continuar el registro del paquete si servicio-cliente está caído
    public void fallbackVerificarCliente(String dni, Throwable t) {
        System.err.println("WARNING (Resiliencia): El servicio-cliente no respondió al verificar DNI: "
                + dni + ". Detalle: " + t.getMessage() + ". Procediendo de forma degradada.");
    }

    // Circuit Breaker & Retry para obtención de datos del cliente
    //Resilience4j intercepta la excepción vía AOP y activa el fallback
    @CircuitBreaker(name = "clienteCB", fallbackMethod = "fallbackObtenerClienteDto")
    @Retry(name = "clienteRetry")
    public ClienteDto obtenerClienteDto(String dni) {
        ApiResponse<ClienteDto> apiResp = clienteClient.obtenerClientePorDni(dni);
        if (apiResp != null && apiResp.isSuccess()) {
            return apiResp.getData();
        }
        throw new IllegalStateException("Respuesta inválida de servicio-cliente al obtener cliente con DNI: " + dni);
    }

    // Fallback degradado para obtención de cliente
    public ClienteDto fallbackObtenerClienteDto(String dni, Throwable t) {
        System.err.println("WARNING (Resiliencia): Fallback al obtener cliente con DNI: "
                + dni + ". Detalle: " + t.getMessage());
        return crearClienteDtoFallback(dni);
    }

    // Circuit Breaker & Retry para búsqueda de clientes
    //Resilience4j intercepta la excepción vía AOP y activa el fallback
    @CircuitBreaker(name = "clienteCB", fallbackMethod = "fallbackBuscarClientes")
    @Retry(name = "clienteRetry")
    public List<ClienteDto> buscarClientesFeign(String texto) {
        ApiResponse<List<ClienteDto>> apiResp = clienteClient.buscarClientes(texto);
        if (apiResp != null && apiResp.isSuccess()) {
            return apiResp.getData();
        }
        throw new IllegalStateException("Respuesta inválida de servicio-cliente al buscar clientes por texto: " + texto);
    }

    // Fallback degradado para búsqueda de clientes
    public List<ClienteDto> fallbackBuscarClientes(String texto, Throwable t) {
        System.err.println("WARNING (Resiliencia): Fallback al buscar clientes por texto: "
                + texto + ". Detalle: " + t.getMessage());
        return new ArrayList<>();
    }

    // Circuit Breaker & Retry para búsqueda de cliente por email (Para filtrado de ROLE_CLIENTE)
    //Resilience4j intercepta la excepción vía AOP y activa el fallback
    @CircuitBreaker(name = "clienteCB", fallbackMethod = "fallbackObtenerClientePorEmail")
    @Retry(name = "clienteRetry")
    public ClienteDto obtenerClientePorEmail(String email) {
        ApiResponse<ClienteDto> apiResp = clienteClient.obtenerClientePorEmail(email);
        if (apiResp != null && apiResp.isSuccess()) {
            return apiResp.getData();
        }
        throw new IllegalStateException("Respuesta inválida de servicio-cliente al obtener cliente por email: " + email);
    }

    // Fallback degradado para obtención de cliente por email
    public ClienteDto fallbackObtenerClientePorEmail(String email, Throwable t) {
        System.err.println("WARNING (Resiliencia): Fallback al obtener cliente por email: "
                + email + ". Detalle: " + t.getMessage());
        return null;
    }

    private ClienteDto crearClienteDtoFallback(String dni) {
        ClienteDto fallback = new ClienteDto();
        fallback.setDni(dni);
        fallback.setNombre("No Disponible");
        fallback.setApellidoPaterno("(Servicio Caído)");
        fallback.setApellidoMaterno("");
        fallback.setEmail("no-disponible@email.com");
        return fallback;
    }
}
