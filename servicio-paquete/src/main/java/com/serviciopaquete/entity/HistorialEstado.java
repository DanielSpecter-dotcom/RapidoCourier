package com.serviciopaquete.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "historial_estados")
public class HistorialEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fechaCambio;
    private String usuarioResponsable;

    @ManyToOne
    @JoinColumn(name = "paquete_id")
    private Paquete paquete;
}