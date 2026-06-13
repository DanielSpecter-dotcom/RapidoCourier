package com.serviciocliente.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReniecResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializaCamposDeDecolecta() throws Exception {
        String json = """
                {
                  "first_name": "DANIEL ALONSO",
                  "first_last_name": "TASAYCO",
                  "second_last_name": "SARAVIA",
                  "full_name": "TASAYCO SARAVIA DANIEL ALONSO",
                  "document_number": "71297556"
                }
                """;

        ReniecResponse response = objectMapper.readValue(json, ReniecResponse.class);

        assertThat(response.nombres()).isEqualTo("DANIEL ALONSO");
        assertThat(response.apellidoPaterno()).isEqualTo("TASAYCO");
        assertThat(response.apellidoMaterno()).isEqualTo("SARAVIA");
        assertThat(response.numeroDocumento()).isEqualTo("71297556");
    }
}
