package com.serviciopaquete.service;

import com.serviciopaquete.dto.request.CreatePaqueteRequest;
import com.serviciopaquete.dto.request.UpdatePaqueteRequest;
import com.serviciopaquete.dto.response.ClienteDto;
import com.serviciopaquete.dto.response.PaqueteResponse;
import com.serviciopaquete.dto.response.CategoriaResponse;
import com.serviciopaquete.dto.response.HistorialEstadoResponse;
import com.serviciopaquete.entity.Categoria;
import com.serviciopaquete.entity.HistorialEstado;
import com.serviciopaquete.entity.Paquete;
import com.serviciopaquete.repository.CategoriaRepository;
import com.serviciopaquete.repository.PaqueteRepository;
import com.serviciopaquete.config.TarifaProperties;
import com.serviciopaquete.exception.ConflictException;
import com.serviciopaquete.exception.ResourceNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaqueteService {

    private final PaqueteRepository paqueteRepository;
    private final CategoriaRepository categoriaRepository;
    private final ClienteLookupService clienteLookupService;
    private final TarifaProperties tarifaProperties;
    private final ModelMapper modelMapper;

    public PaqueteService(PaqueteRepository paqueteRepository,
                          CategoriaRepository categoriaRepository,
                          ClienteLookupService clienteLookupService,
                          TarifaProperties tarifaProperties,
                          ModelMapper modelMapper) {
        this.paqueteRepository = paqueteRepository;
        this.categoriaRepository = categoriaRepository;
        this.clienteLookupService = clienteLookupService;
        this.tarifaProperties = tarifaProperties;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public PaqueteResponse crearPaquete(CreatePaqueteRequest request, String emailOperador) {
        // Validación de comunicación inter-servicio con resiliencia en bean separado
        clienteLookupService.verificarClienteExistente(request.getDniRemitente());
        clienteLookupService.verificarClienteExistente(request.getDniDestinatario());

        // Conversión entidad DTO mediante ModelMapper
        Paquete paquete = modelMapper.map(request, Paquete.class);
        
        paquete.setCodigoRastreo(generarCodigoRastreoUnico());
        paquete.setEstadoActual("REGISTRADO");

        //Cálculo automático de tarifa dinámico vía RefreshScope
        calcularYAsignarTarifa(paquete);

        //Registro automático de historial
        HistorialEstado historial = new HistorialEstado();
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo("REGISTRADO");
        historial.setFechaCambio(LocalDateTime.now());
        historial.setUsuarioResponsable(emailOperador != null ? emailOperador : "OPERADOR_DEFECTO");
        historial.setPaquete(paquete);
        
        List<HistorialEstado> listaHistorial = new ArrayList<>();
        listaHistorial.add(historial);
        paquete.setHistorial(listaHistorial);

        Paquete paqueteGuardado = paqueteRepository.save(paquete);
        return mapearAPaqueteResponse(paqueteGuardado);
    }

    @Transactional(readOnly = true)
    public PaqueteResponse obtenerPaquetePorId(UUID id, String userEmail, String userRole) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el ID: " + id));
        
        validarPropiedadCliente(paquete, userEmail, userRole);
        
        return mapearAPaqueteResponse(paquete);
    }

    @Transactional(readOnly = true)
    public PaqueteResponse rastrearPaquete(String codigo, String userEmail, String userRole) {
        Paquete paquete = paqueteRepository.findByCodigoRastreo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el código de rastreo: " + codigo));
        
        validarPropiedadCliente(paquete, userEmail, userRole);
        
        return mapearAPaqueteResponse(paquete);
    }

    @Transactional
    public PaqueteResponse actualizarPaquete(UUID id, UpdatePaqueteRequest request) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el ID: " + id));

        if (request.getPesoKg() != null) {
            paquete.setPesoKg(request.getPesoKg());
        }
        if (request.getValorDeclarado() != null) {
            paquete.setValorDeclarado(request.getValorDeclarado());
        }
        if (request.getSucursalOrigen() != null && !request.getSucursalOrigen().trim().isEmpty()) {
            paquete.setSucursalOrigen(request.getSucursalOrigen().trim());
        }
        if (request.getSucursalDestino() != null && !request.getSucursalDestino().trim().isEmpty()) {
            paquete.setSucursalDestino(request.getSucursalDestino().trim());
        }
        if (request.getDescripcion() != null) {
            paquete.setDescripcion(request.getDescripcion().trim());
        }

        // Recalcular tarifa tras actualizaciones
        calcularYAsignarTarifa(paquete);

        Paquete paqueteActualizado = paqueteRepository.save(paquete);
        return mapearAPaqueteResponse(paqueteActualizado);
    }

    @Transactional
    public void eliminarPaquete(UUID id) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el ID: " + id));
        paqueteRepository.delete(paquete);
    }

    @Transactional
    public PaqueteResponse actualizarEstado(UUID id, String nuevoEstado, String emailOperador) {
        Paquete paquete = paqueteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el ID: " + id));

        String estadoActual = paquete.getEstadoActual();

        // Máquina de estados
        boolean transicionValida = false;
        if ("REGISTRADO".equals(estadoActual) && "EN_TRANSITO".equals(nuevoEstado)) {
            transicionValida = true;
        } else if ("EN_TRANSITO".equals(estadoActual) && "ENTREGADO".equals(nuevoEstado)) {
            transicionValida = true;
        }

        if (!transicionValida) {
            throw new ConflictException("Transición de estado inválida de " + estadoActual + " a " + nuevoEstado);
        }

        paquete.setEstadoActual(nuevoEstado);

        //Historial automático
        HistorialEstado nuevoHistorial = new HistorialEstado();
        nuevoHistorial.setEstadoAnterior(estadoActual);
        nuevoHistorial.setEstadoNuevo(nuevoEstado);
        nuevoHistorial.setFechaCambio(LocalDateTime.now());
        nuevoHistorial.setUsuarioResponsable(emailOperador != null ? emailOperador : "OPERADOR_DEFECTO");
        nuevoHistorial.setPaquete(paquete);

        if (paquete.getHistorial() == null) {
            paquete.setHistorial(new ArrayList<>());
        }
        paquete.getHistorial().add(nuevoHistorial);

        Paquete paqueteActualizado = paqueteRepository.save(paquete);
        return mapearAPaqueteResponse(paqueteActualizado);
    }

    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> obtenerHistorial(UUID paqueteId, String userEmail, String userRole) {
        Paquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el ID: " + paqueteId));
        
        validarPropiedadCliente(paquete, userEmail, userRole);

        return paquete.getHistorial().stream()
                .sorted((h1, h2) -> h1.getFechaCambio().compareTo(h2.getFechaCambio()))
                .map(h -> modelMapper.map(h, HistorialEstadoResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public PaqueteResponse asignarCategoria(UUID paqueteId, UUID categoriaId) {
        Paquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado con el ID: " + paqueteId));

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con el ID: " + categoriaId));

        if (paquete.getCategorias() == null) {
            paquete.setCategorias(new ArrayList<>());
        }

        // Evitar duplicados
        if (!paquete.getCategorias().contains(categoria)) {
            paquete.getCategorias().add(categoria);
        }

        Paquete paqueteActualizado = paqueteRepository.save(paquete);
        return mapearAPaqueteResponse(paqueteActualizado);
    }

    @Transactional(readOnly = true)
    public List<PaqueteResponse> buscarYFiltrarPaquetes(String texto, String sucursal, String estado, String userEmail, String userRole) {
        List<Paquete> paquetes;

        String dniCliente = null;
        if ("ROLE_CLIENTE".equalsIgnoreCase(userRole)) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return new ArrayList<>();
            }
            ClienteDto cliente = clienteLookupService.obtenerClientePorEmail(userEmail);
            if (cliente == null) {
                return new ArrayList<>();
            }
            dniCliente = cliente.getDni();
        }

        if (texto != null && !texto.trim().isEmpty()) {
            texto = texto.trim();
            //Búsqueda por texto parcial inter-servicio
            if (texto.toUpperCase().startsWith("RC-")) {
                paquetes = paqueteRepository.buscarPorTextoParcial(texto);
            } else {
                List<ClienteDto> clientes = clienteLookupService.buscarClientesFeign(texto);
                if (!clientes.isEmpty()) {
                    List<String> dnis = clientes.stream().map(ClienteDto::getDni).collect(Collectors.toList());
                    paquetes = paqueteRepository.buscarPorCodigoODnis(texto, dnis);
                } else {
                    paquetes = paqueteRepository.buscarPorTextoParcial(texto);
                }
            }
        } else if (sucursal != null && !sucursal.trim().isEmpty() && estado != null && !estado.trim().isEmpty()) {
            paquetes = paqueteRepository.findBySucursalAndEstado(sucursal.trim(), estado.trim());
        } else {
            paquetes = paqueteRepository.findAll();
        }

        // Filtro combinado en memoria si se proporcionaron todos los parámetros (texto y sucursal/estado)
        if (texto != null && !texto.trim().isEmpty() && sucursal != null && !sucursal.trim().isEmpty() && estado != null && !estado.trim().isEmpty()) {
            paquetes = paquetes.stream()
                    .filter(p -> (sucursal.trim().equalsIgnoreCase(p.getSucursalOrigen()) || sucursal.trim().equalsIgnoreCase(p.getSucursalDestino()))
                            && estado.trim().equalsIgnoreCase(p.getEstadoActual()))
                    .collect(Collectors.toList());
        }

        // Filtrar por DNI si es ROLE_CLIENTE
        if (dniCliente != null) {
            final String finalDni = dniCliente;
            paquetes = paquetes.stream()
                    .filter(p -> finalDni.equals(p.getDniRemitente()) || finalDni.equals(p.getDniDestinatario()))
                    .collect(Collectors.toList());
        }

        return paquetes.stream()
                .map(this::mapearAPaqueteResponse)
                .collect(Collectors.toList());
    }

    // --- Métodos Auxiliares ---

    private void calcularYAsignarTarifa(Paquete paquete) {
        BigDecimal precioBase = tarifaProperties.getPrecioBase();
        BigDecimal factorPeso = tarifaProperties.getFactorPeso();
        
        BigDecimal costoPeso = new BigDecimal(paquete.getPesoKg()).multiply(factorPeso);
        BigDecimal costoSeguro = paquete.getValorDeclarado().multiply(new BigDecimal("0.02"));
        paquete.setTarifa(precioBase.add(costoPeso).add(costoSeguro));
    }

    private PaqueteResponse mapearAPaqueteResponse(Paquete paquete) {
        PaqueteResponse response = modelMapper.map(paquete, PaqueteResponse.class);

        // Cargar datos de clientes vía Feign utilizando ClienteLookupService (Resiliencia con AOP proxy activo)
        response.setRemitente(clienteLookupService.obtenerClienteDto(paquete.getDniRemitente()));
        response.setDestinatario(clienteLookupService.obtenerClienteDto(paquete.getDniDestinatario()));

        // Cargar categorías si existen
        if (paquete.getCategorias() != null) {
            response.setCategorias(paquete.getCategorias().stream()
                    .map(c -> modelMapper.map(c, CategoriaResponse.class))
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private void validarPropiedadCliente(Paquete paquete, String userEmail, String userRole) {
        if ("ROLE_CLIENTE".equalsIgnoreCase(userRole)) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                throw new ConflictException("Acceso denegado: Email de usuario no proporcionado.");
            }
            ClienteDto cliente = clienteLookupService.obtenerClientePorEmail(userEmail);
            if (cliente == null) {
                throw new ConflictException("Acceso denegado: Cliente no registrado para el usuario autenticado.");
            }
            String dniCliente = cliente.getDni();
            if (!dniCliente.equals(paquete.getDniRemitente()) && !dniCliente.equals(paquete.getDniDestinatario())) {
                throw new ConflictException("Acceso denegado: No tienes permisos para visualizar este paquete.");
            }
        }
    }

    // RF-03 / Sección 2: Garantiza unicidad del código de rastreo consultando BD antes de asignar
    private String generarCodigoRastreoUnico() {
        String codigo;
        do {
            codigo = "RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paqueteRepository.findByCodigoRastreo(codigo).isPresent());
        return codigo;
    }
}