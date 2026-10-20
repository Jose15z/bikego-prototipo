package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Bicicleta {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private EstadoBicicleta estado;

    /** Nivel de bateria en porcentaje (0 a 100). */
    private int bateria;

    private double latitud;
    private double longitud;

    @ManyToOne
    private Estacion estacion;

    /** Solo para el prototipo: fuerza que el candado IoT simulado no responda (demostracion de SWR-02.5). */
    private boolean candadoConFalla;

    protected Bicicleta() { }

    public Bicicleta(String id, EstadoBicicleta estado, int bateria, Estacion estacion,
                     double latitud, double longitud, boolean candadoConFalla) {
        this.id = id;
        this.estado = estado;
        this.bateria = bateria;
        this.estacion = estacion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.candadoConFalla = candadoConFalla;
    }

    /** RN-002, RN-003 y RN-005: solo se ofrece si esta Disponible y con bateria suficiente. */
    public boolean estaDisponible(int bateriaMinima) {
        return estado == EstadoBicicleta.DISPONIBLE && bateria >= bateriaMinima;
    }

    public void marcarEnUso() { this.estado = EstadoBicicleta.EN_USO; }
    public void mantenimiento() { estado = EstadoBicicleta.EN_MANTENIMIENTO; }
    public void habilitar(int bateria) { this.bateria = bateria; estado = EstadoBicicleta.DISPONIBLE; }
    public void devolver(Estacion estacion, double lat, double lng) {
        this.estacion = estacion; latitud = lat; longitud = lng;
        if (estado != EstadoBicicleta.EN_MANTENIMIENTO) estado = EstadoBicicleta.DISPONIBLE;
    }

    /** Actualiza solo la posición; conserva estado, batería y estación asignada. */
    public void reubicar(double latitud, double longitud) {
        this.latitud = latitud; this.longitud = longitud;
    }

    public String getId() { return id; }
    public EstadoBicicleta getEstado() { return estado; }
    public int getBateria() { return bateria; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public Estacion getEstacion() { return estacion; }
    public boolean isCandadoConFalla() { return candadoConFalla; }
}
