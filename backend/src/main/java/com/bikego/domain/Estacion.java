package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Estacion {

    @Id
    private String id;
    private String nombre;
    private double latitud;
    private double longitud;

    protected Estacion() { }

    public Estacion(String id, String nombre, double latitud, double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
}
