package com.serviciopaquete.repository;

import com.serviciopaquete.entity.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaqueteRepository extends JpaRepository<Paquete, UUID> {

    Optional<Paquete> findByCodigoRastreo(String codigoRastreo);

    // RF-06: Filtro por sucursal y estado (NATIVO)
    @Query(value = "SELECT * FROM paquetes WHERE (sucursal_origen = :sucursal OR sucursal_destino = :sucursal) AND estado_actual = :estado", nativeQuery = true)
    List<Paquete> findBySucursalAndEstado(@Param("sucursal") String sucursal, @Param("estado") String estado);

    // RF-07: Búsqueda por texto (código de rastreo o DNIs remitente/destinatario) (NATIVO)
    @Query(value = "SELECT * FROM paquetes WHERE LOWER(codigo_rastreo) LIKE LOWER(CONCAT('%', :texto, '%')) OR dni_remitente = :texto OR dni_destinatario = :texto", nativeQuery = true)
    List<Paquete> buscarPorTextoParcial(@Param("texto") String texto);

    // RF-07: Búsqueda ampliada por código o lista de DNIs de clientes (NATIVO)
    @Query(value = "SELECT * FROM paquetes WHERE LOWER(codigo_rastreo) LIKE LOWER(CONCAT('%', :texto, '%')) OR dni_remitente IN (:dnis) OR dni_destinatario IN (:dnis)", nativeQuery = true)
    List<Paquete> buscarPorCodigoODnis(@Param("texto") String texto, @Param("dnis") List<String> dnis);

    // Sección 4: JOIN nativo que combina paquetes e historial de estados
    @Query(value = "SELECT DISTINCT p.* FROM paquetes p INNER JOIN historial_estados h ON p.id = h.paquete_id WHERE h.estado_nuevo = :estado", nativeQuery = true)
    List<Paquete> findPaquetesByHistoricoEstado(@Param("estado") String estado);
}