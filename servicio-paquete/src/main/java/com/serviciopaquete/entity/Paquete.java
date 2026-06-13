package com.serviciopaquete.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "paquetes")
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String codigoRastreo;
    private String dniRemitente;
    private String dniDestinatario;
    private Double pesoKg;
    private BigDecimal valorDeclarado;
    private BigDecimal tarifa;
    private String sucursalOrigen;
    private String sucursalDestino;
    private String estadoActual; // ej. REGISTRADO, EN_TRANSITO, ENTREGADO
    private String descripcion;

    // RNF-04: Campos de auditoría
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Relación OneToMany (RF-05: Historial)
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL)
    private List<HistorialEstado> historial;

    // Relación ManyToMany (RF-09: Categorías)
    @ManyToMany
    @JoinTable(
            name = "paquete_categoria",
            joinColumns = @JoinColumn(name = "paquete_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private List<Categoria> categorias;

}