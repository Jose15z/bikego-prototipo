package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Usuario {

    @Id
    private String id;
    private String nombre;
    private String correo;

    @Enumerated(EnumType.STRING)
    private EstadoCuenta estadoCuenta;

    /** Valor adeudado en COP. Mayor que cero significa deuda pendiente (RN-001). */
    private int deudaPendiente;

    /** Bloqueo temporal por pago rechazado (RN-008). Null si no esta bloqueado. */
    private LocalDateTime bloqueadoHasta;

    /** Token entregado por la pasarela. Nunca se guarda el numero completo de la tarjeta. */
    private String tokenPago;
    private String marcaTarjeta;
    private String ultimos4;

    /** Texto corto que explica el escenario de prueba (solo prototipo). */
    private String escenario;

    protected Usuario() { }

    public Usuario(String id, String nombre, String correo, EstadoCuenta estadoCuenta, int deudaPendiente,
                   String tokenPago, String marcaTarjeta, String ultimos4, String escenario) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.estadoCuenta = estadoCuenta;
        this.deudaPendiente = deudaPendiente;
        this.tokenPago = tokenPago;
        this.marcaTarjeta = marcaTarjeta;
        this.ultimos4 = ultimos4;
        this.escenario = escenario;
    }

    public boolean tieneMetodoPago() { return tokenPago != null && !tokenPago.isBlank(); }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    public void bloquearHasta(LocalDateTime hasta) { this.bloqueadoHasta = hasta; }

    public void actualizarMetodoPago(String token, String marca, String ultimos4) {
        this.tokenPago = token;
        this.marcaTarjeta = marca;
        this.ultimos4 = ultimos4;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public int getDeudaPendiente() { return deudaPendiente; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public String getTokenPago() { return tokenPago; }
    public String getMarcaTarjeta() { return marcaTarjeta; }
    public String getUltimos4() { return ultimos4; }
    public String getEscenario() { return escenario; }
}
