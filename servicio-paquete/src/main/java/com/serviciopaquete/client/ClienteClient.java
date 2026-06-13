package com.serviciopaquete.client;

import com.serviciopaquete.dto.ApiResponse;
import com.serviciopaquete.dto.response.ClienteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

// Busca en Eureka el servicio llamado "servicio-cliente"
@FeignClient(name = "servicio-cliente", path = "/api/v1/clientes")
public interface ClienteClient {

    @GetMapping("/{dni}")
    ApiResponse<ClienteDto> obtenerClientePorDni(@PathVariable("dni") String dni);

    @GetMapping("/buscar")
    ApiResponse<List<ClienteDto>> buscarClientes(@RequestParam("texto") String texto);

    @GetMapping("/email/{email}")
    ApiResponse<ClienteDto> obtenerClientePorEmail(@PathVariable("email") String email);
}