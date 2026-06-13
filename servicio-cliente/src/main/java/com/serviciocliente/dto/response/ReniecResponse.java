package com.serviciocliente.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReniecResponse(
        @JsonProperty("first_name")
        String nombres,
        @JsonProperty("first_last_name")
        String apellidoPaterno,
        @JsonProperty("second_last_name")
        String apellidoMaterno,
        @JsonProperty("document_number")
        String numeroDocumento
) {}
