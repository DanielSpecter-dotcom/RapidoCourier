package com.servicioauth.config;

import com.servicioauth.entity.RolNombre;
import com.servicioauth.entity.Usuario;
import com.servicioauth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyección por constructor (Requisito del examen)
    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario(null, "Admin Principal", "admin@rapidocourier.pe", passwordEncoder.encode("Admin123!"), RolNombre.ROLE_ADMIN);
            Usuario operador = new Usuario(null, "Operador Uno", "operador@rapidocourier.pe", passwordEncoder.encode("Operador123!"), RolNombre.ROLE_OPERADOR);
            Usuario cliente = new Usuario(null, "Cliente Test", "cliente@correo.com", passwordEncoder.encode("Cliente123!"), RolNombre.ROLE_CLIENTE);

            usuarioRepository.save(admin);
            usuarioRepository.save(operador);
            usuarioRepository.save(cliente);

            System.out.println("Usuarios base inicializados correctamente.");
        }
    }
}