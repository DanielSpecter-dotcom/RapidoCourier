package com.serviciopaquete.controller;

import com.serviciopaquete.dto.ApiResponse;
import com.serviciopaquete.dto.request.CategoriaRequest;
import com.serviciopaquete.dto.response.CategoriaResponse;
import com.serviciopaquete.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse response = categoriaService.crearCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Categoría creada exitosamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listar() {
        List<CategoriaResponse> response = categoriaService.listarCategorias();
        return ResponseEntity.ok(new ApiResponse<>(true, "Listado de categorías recuperado exitosamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> obtenerPorId(@PathVariable UUID id) {
        CategoriaResponse response = categoriaService.obtenerPorId(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categoría encontrada", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse response = categoriaService.actualizarCategoria(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categoría actualizada exitosamente", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        categoriaService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
