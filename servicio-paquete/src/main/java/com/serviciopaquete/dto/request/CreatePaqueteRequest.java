package com.serviciopaquete.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class CreatePaqueteRequest {

    @NotBlank(message = "El DNI del remitente es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "DNI de remitente inválido")
    private String dniRemitente;

    @NotBlank(message = "El DNI del destinatario es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "DNI de destinatario inválido")
    private String dniDestinatario;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser mayor a 0")
    private Double pesoKg;

    @NotNull(message = "El valor declarado es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El valor declarado no puede ser negativo")
    private BigDecimal valorDeclarado;

    @NotBlank(message = "La sucursal de origen es obligatoria")
    @Size(min = 2, max = 50, message = "La sucursal de origen debe tener entre 2 y 50 caracteres")
    private String sucursalOrigen;

    @NotBlank(message = "La sucursal de destino es obligatoria")
    @Size(min = 2, max = 50, message = "La sucursal de destino debe tener entre 2 y 50 caracteres")
    private String sucursalDestino;

    private String descripcion;
}
