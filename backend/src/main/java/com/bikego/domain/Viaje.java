package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/** Registro historico del viaje (RN-006, SWR-02.6). */
@Entity
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idViaje;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Bicicleta bicicleta;

    private LocalDateTime horaInicio;

    @Enumerated(EnumType.STRING)
    private EstadoViaje estado;

    protected Viaje() { }

    public Viaje(Usuario usuario, Bicicleta bicicleta, LocalDateTime horaInicio) {
        this.usuario = usuario;
        this.bicicleta = bicicleta;
        this.horaInicio = horaInicio;
        this.estado = EstadoViaje.EN_CURSO;
    }

    public Long getIdViaje() { return idViaje; }
    public Usuario getUsuario() { return usuario; }
    public Bicicleta getBicicleta() { return bicicleta; }
    public LocalDateTime getHoraInicio() { return horaInicio; }
    public EstadoViaje getEstado() { return estado; }
}
