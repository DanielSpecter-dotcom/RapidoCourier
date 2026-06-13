package com.serviciopaquete.config;

import com.serviciopaquete.entity.Categoria;
import com.serviciopaquete.repository.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    public DataInitializer(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoriaRepository.count() == 0) {
            List<String> nombresCategorias = List.of(
                    "FRAGIL",
                    "REFRIGERADO",
                    "DOCUMENTOS",
                    "SOBREDIMENSIONADO"
            );

            for (String nombre : nombresCategorias) {
                Categoria categoria = new Categoria();
                categoria.setNombre(nombre);
                categoriaRepository.save(categoria);
            }
            System.out.println("DataInitializer: Categorías base inicializadas exitosamente en la base de datos.");
        }
    }
}
