package com.serviciopaquete.controller;

import com.serviciopaquete.dto.ApiResponse;
import com.serviciopaquete.dto.request.CreatePaqueteRequest;
import com.serviciopaquete.dto.request.UpdateEstadoRequest;
import com.serviciopaquete.dto.request.UpdatePaqueteRequest;
import com.serviciopaquete.dto.response.HistorialEstadoResponse;
import com.serviciopaquete.dto.response.PaqueteResponse;
import com.serviciopaquete.service.PaqueteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/paquetes")
public class PaqueteController {

    private final PaqueteService paqueteService;

    public PaqueteController(PaqueteService paqueteService) {
        this.paqueteService = paqueteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaqueteResponse>> crearEnvio(
            @Valid @RequestBody CreatePaqueteRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String emailOperador) {
        
        PaqueteResponse response = paqueteService.crearPaquete(request, emailOperador);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Paquete registrado exitosamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaqueteResponse>> obtenerPorId(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        PaqueteResponse response = paqueteService.obtenerPaquetePorId(id, userEmail, userRole);
        return ResponseEntity.ok(new ApiResponse<>(true, "Paquete encontrado", response));
    }

    @GetMapping("/rastreo/{codigo}")
    public ResponseEntity<ApiResponse<PaqueteResponse>> rastrearEnvio(
            @PathVariable String codigo,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        PaqueteResponse response = paqueteService.rastrearPaquete(codigo, userEmail, userRole);
        return ResponseEntity.ok(new ApiResponse<>(true, "Paquete encontrado", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaqueteResponse>> actualizarEnvio(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaqueteRequest request) {
        
        PaqueteResponse response = paqueteService.actualizarPaquete(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Paquete actualizado exitosamente", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEnvio(@PathVariable UUID id) {
        paqueteService.eliminarPaquete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<PaqueteResponse>> actualizarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEstadoRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String emailOperador) {
        
        PaqueteResponse response = paqueteService.actualizarEstado(id, request.getEstadoNuevo(), emailOperador);
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de paquete actualizado exitosamente", response));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<ApiResponse<List<HistorialEstadoResponse>>> obtenerHistorial(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        List<HistorialEstadoResponse> response = paqueteService.obtenerHistorial(id, userEmail, userRole);
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de estados recuperado exitosamente", response));
    }

    @PostMapping("/{id}/categorias/{categoriaId}")
    public ResponseEntity<ApiResponse<PaqueteResponse>> asignarCategoria(
            @PathVariable UUID id,
            @PathVariable UUID categoriaId) {
        
        PaqueteResponse response = paqueteService.asignarCategoria(id, categoriaId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categoría asignada exitosamente al paquete", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> buscarYFiltrar(
            @RequestParam(value = "texto", required = false) String texto,
            @RequestParam(value = "busqueda", required = false) String busqueda,
            @RequestParam(value = "sucursal", required = false) String sucursal,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        // Alias: si viene 'busqueda' y no 'texto', usar 'busqueda'
        String textoBusqueda = (texto != null) ? texto : busqueda;
        
        List<PaqueteResponse> response = paqueteService.buscarYFiltrarPaquetes(textoBusqueda, sucursal, estado, userEmail, userRole);
        return ResponseEntity.ok(new ApiResponse<>(true, "Búsqueda y filtrado completados exitosamente", response));
    }
}