package com.serviciopaquete.service;

import com.serviciopaquete.dto.request.CategoriaRequest;
import com.serviciopaquete.dto.response.CategoriaResponse;
import com.serviciopaquete.entity.Categoria;
import com.serviciopaquete.repository.CategoriaRepository;
import com.serviciopaquete.exception.ConflictException;
import com.serviciopaquete.exception.ResourceNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ModelMapper modelMapper;

    public CategoriaService(CategoriaRepository categoriaRepository, ModelMapper modelMapper) {
        this.categoriaRepository = categoriaRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest request) {
        String nombreNormalizado = request.getNombre().trim().toUpperCase();
        if (categoriaRepository.findByNombre(nombreNormalizado).isPresent()) {
            throw new ConflictException("Ya existe una categoría con el nombre: " + nombreNormalizado);
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombreNormalizado);
        
        Categoria guardada = categoriaRepository.save(categoria);
        return modelMapper.map(guardada, CategoriaResponse.class);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con el ID: " + id));
        return modelMapper.map(categoria, CategoriaResponse.class);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> modelMapper.map(c, CategoriaResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaResponse actualizarCategoria(UUID id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con el ID: " + id));

        String nombreNormalizado = request.getNombre().trim().toUpperCase();
        categoriaRepository.findByNombre(nombreNormalizado).ifPresent(c -> {
            if (!c.getId().equals(id)) {
                throw new ConflictException("Ya existe otra categoría con el nombre: " + nombreNormalizado);
            }
        });

        categoria.setNombre(nombreNormalizado);
        Categoria actualizada = categoriaRepository.save(categoria);
        return modelMapper.map(actualizada, CategoriaResponse.class);
    }

    @Transactional
    public void eliminarCategoria(UUID id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con el ID: " + id));
        categoriaRepository.delete(categoria);
    }
}
