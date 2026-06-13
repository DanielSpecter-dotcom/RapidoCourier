package com.serviciocliente.controller;

import com.serviciocliente.dto.request.CreateClienteRequest;
import com.serviciocliente.dto.response.ApiResponse;
import com.serviciocliente.dto.response.ClienteResponse;
import com.serviciocliente.entity.Cliente;
import com.serviciocliente.service.ClienteService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final ModelMapper modelMapper;

    public ClienteController(ClienteService clienteService, ModelMapper modelMapper) {
        this.clienteService = clienteService;
        this.modelMapper = modelMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> registrar(@Valid @RequestBody CreateClienteRequest request) {
        Cliente clienteRegistrado = clienteService.registrarCliente(request);
        ClienteResponse response = modelMapper.map(clienteRegistrado, ClienteResponse.class);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Cliente registrado exitosamente con datos de RENIEC", response));
    }

    @GetMapping("/{dni}")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerPorDni(@PathVariable String dni) {
        Cliente cliente = clienteService.buscarPorDni(dni);
        ClienteResponse response = modelMapper.map(cliente, ClienteResponse.class);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cliente encontrado", response));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<java.util.List<ClienteResponse>>> buscarClientes(@RequestParam("texto") String texto) {
        java.util.List<Cliente> clientes = clienteService.buscarPorTexto(texto);
        java.util.List<ClienteResponse> response = clientes.stream()
                .map(c -> modelMapper.map(c, ClienteResponse.class))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Búsqueda de clientes exitosa", response));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerPorEmail(@PathVariable String email) {
        Cliente cliente = clienteService.buscarPorEmail(email);
        ClienteResponse response = modelMapper.map(cliente, ClienteResponse.class);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cliente encontrado por email", response));
    }
}