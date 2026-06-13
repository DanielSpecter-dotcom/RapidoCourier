package com.serviciopaquete.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class PaqueteResponse {
    private UUID id;
    private String codigoRastreo;
    private String dniRemitente;
    private String dniDestinatario;
    
    // Objetos de cliente completos obtenidos vía Feign
    private ClienteDto remitente;
    private ClienteDto destinatario;
    
    private Double pesoKg;
    private BigDecimal valorDeclarado;
    private BigDecimal tarifa;
    private String sucursalOrigen;
    private String sucursalDestino;
    private String estadoActual;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<CategoriaResponse> categorias;
}
