package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/** Evento de rechazo de pago registrado por SWR-02.7. */
@Entity
public class RechazoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    private String motivo;
    private LocalDateTime fecha;
    private LocalDateTime bloqueadoHasta;

    protected RechazoPago() { }

    public RechazoPago(Usuario usuario, String motivo, LocalDateTime fecha, LocalDateTime bloqueadoHasta) {
        this.usuario = usuario;
        this.motivo = motivo;
        this.fecha = fecha;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFecha() { return fecha; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
}
