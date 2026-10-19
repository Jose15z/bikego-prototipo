package com.bikego.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class EventoFlota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String clase;
    public String bicicletaId;
    public String responsableId;
    public String responsable;
    public Long viajeId;
    public LocalDateTime fecha;
    public LocalDateTime cierre;
    public String tipo;
    @Column(length = 2000) public String descripcion;
    public Double latitud;
    public Double longitud;
    public String estado = "ABIERTO";
    public EventoFlota() {}
}
