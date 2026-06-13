package com.serviciocliente.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ClienteResponse {
    private UUID id;
    private String dni;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private String telefono;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
