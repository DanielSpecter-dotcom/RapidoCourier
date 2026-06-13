package com.serviciocliente.repository;

import com.serviciocliente.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByDni(String dni);
    Optional<Cliente> findByEmail(String email);
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Cliente> buscarPorTexto(@Param("texto") String texto);
}