package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Estacion {

    @Id
    private String id;
    private String nombre;
    private String referencia;
    private double latitud;
    private double longitud;

    protected Estacion() { }

    public Estacion(String id, String nombre, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public void reubicar(String nombre, double latitud, double longitud) {
        this.nombre = nombre; this.latitud = latitud; this.longitud = longitud;
    }

    public void referencia(String referencia) { this.referencia = referencia; }
    public String getReferencia() { return referencia; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}
