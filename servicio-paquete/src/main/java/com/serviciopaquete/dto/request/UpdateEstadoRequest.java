package com.serviciopaquete.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateEstadoRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String estadoNuevo;
}
