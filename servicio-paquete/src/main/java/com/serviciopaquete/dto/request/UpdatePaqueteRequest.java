package com.serviciopaquete.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class UpdatePaqueteRequest {

    @Positive(message = "El peso debe ser mayor a 0")
    private Double pesoKg;

    @DecimalMin(value = "0.0", inclusive = true, message = "El valor declarado no puede ser negativo")
    private BigDecimal valorDeclarado;

    @Size(min = 2, max = 50, message = "La sucursal de origen debe tener entre 2 y 50 caracteres")
    private String sucursalOrigen;

    @Size(min = 2, max = 50, message = "La sucursal de destino debe tener entre 2 y 50 caracteres")
    private String sucursalDestino;

    private String descripcion;
}
