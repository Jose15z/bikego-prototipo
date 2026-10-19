package com.bikego.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Usuario {

    @Id
    private String id;
    private String nombre;
    @jakarta.persistence.Column(unique=true)
    private String correo;
    @jakarta.persistence.Column(unique=true)
    private String documento;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String passwordHash;
    private LocalDateTime fechaAceptacionTerminos;
    private String versionTerminos;
    private String codigoVerificacionHash;
    private LocalDateTime codigoVerificacionExpira;

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
    private String rol = "CLIENTE";

    public String getRol() { return rol == null ? "CLIENTE" : rol; }
    public void asignarRol(String rol) { this.rol = rol; }
    public void ajustarDeuda(long diferencia) { deudaPendiente = Math.toIntExact(Math.max(0, deudaPendiente + diferencia)); }

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

    public Usuario(String id, String nombre, String correo, String documento, String telefono,
                   LocalDate fechaNacimiento, String direccion, String passwordHash,
                   LocalDateTime fechaAceptacionTerminos, String versionTerminos,
                   String codigoVerificacionHash, LocalDateTime codigoVerificacionExpira) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.documento = documento;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
        this.passwordHash = passwordHash;
        this.fechaAceptacionTerminos = fechaAceptacionTerminos;
        this.versionTerminos = versionTerminos;
        this.codigoVerificacionHash = codigoVerificacionHash;
        this.codigoVerificacionExpira = codigoVerificacionExpira;
        this.estadoCuenta = EstadoCuenta.PENDIENTE_VERIFICACION;
        this.deudaPendiente = 0;
    }

    public void configurarCredenciales(String documento, String passwordHash) {
        this.documento = documento;
        this.passwordHash = passwordHash;
    }

    public boolean codigoVigente(LocalDateTime ahora) {
        return codigoVerificacionHash != null && codigoVerificacionExpira != null
                && ahora.isBefore(codigoVerificacionExpira);
    }

    public void renovarCodigo(String hash,LocalDateTime expira) { codigoVerificacionHash=hash;codigoVerificacionExpira=expira; }
    public void activar() {
        this.estadoCuenta = EstadoCuenta.ACTIVA;
        this.codigoVerificacionHash = null;
        this.codigoVerificacionExpira = null;
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
    public String getDocumento() { return documento; }
    public String getTelefono() { return telefono; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getDireccion() { return direccion; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getFechaAceptacionTerminos() { return fechaAceptacionTerminos; }
    public String getVersionTerminos() { return versionTerminos; }
    public String getCodigoVerificacionHash() { return codigoVerificacionHash; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public int getDeudaPendiente() { return deudaPendiente; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public String getTokenPago() { return tokenPago; }
    public String getMarcaTarjeta() { return marcaTarjeta; }
    public String getUltimos4() { return ultimos4; }
    public String getEscenario() { return escenario; }
}
