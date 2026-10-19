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
    private LocalDateTime horaFin;
    private Double latitudFinal;
    private Double longitudFinal;
    @jakarta.persistence.Column(columnDefinition="bigint default 0")
    private long minutos;
    @jakarta.persistence.Column(columnDefinition="bigint default 0")
    private long total;
    private String clavePago = java.util.UUID.randomUUID().toString();
    public String getClavePago() { if(clavePago == null) clavePago=java.util.UUID.randomUUID().toString(); return clavePago; }
    @jakarta.persistence.Column(columnDefinition="bigint default 0")
    private long recargo;
    @jakarta.persistence.Column(columnDefinition="boolean default false")
    private boolean conRecargo;
    public boolean isConRecargo() { return conRecargo; }
    @jakarta.persistence.Column(columnDefinition="integer default 0")
    private int tarifaBaseAplicada;
    @jakarta.persistence.Column(columnDefinition="integer default 0")
    private int tarifaMinutoAplicada;
    private String estadoPago;
    @jakarta.persistence.Column(columnDefinition="boolean default false")
    private boolean conIncidente;
    @jakarta.persistence.Column(columnDefinition="boolean default false")
    private boolean deudaRegistrada;

    public void finalizar(LocalDateTime fin, double lat, double lng, int base, int tarifa, long recargo) {
        horaFin = fin; latitudFinal = lat; longitudFinal = lng;
        minutos = (long) Math.ceil(Math.max(0, java.time.Duration.between(horaInicio, fin).getSeconds()) / 60.0);
        tarifaBaseAplicada = base; tarifaMinutoAplicada = tarifa; this.recargo = recargo;
        conRecargo = java.time.Duration.between(horaInicio, fin).getSeconds() > 14400;
        total = base + minutos * tarifa + recargo; estado = EstadoViaje.FINALIZADO;
        estadoPago = conIncidente ? "EN_REVISION" : "PENDIENTE";
    }
    public void revisar() { conIncidente = true; estadoPago = "EN_REVISION"; }
    public void resolver(boolean anular) { conIncidente = false; estadoPago = anular ? "ANULADO" : "PENDIENTE"; }
    public void pagado() { estadoPago = "PROCESADO"; }
    public void deudaRegistrada(boolean valor) { deudaRegistrada = valor; }
    public boolean isDeudaRegistrada() { return deudaRegistrada; }
    public LocalDateTime getHoraFin() { return horaFin; }
    public Double getLatitudFinal() { return latitudFinal; }
    public Double getLongitudFinal() { return longitudFinal; }
    public long getMinutos() { return minutos; }
    public long getTotal() { return total; }
    public long getRecargo() { return recargo; }
    public int getTarifaBaseAplicada() { return tarifaBaseAplicada; }
    public int getTarifaMinutoAplicada() { return tarifaMinutoAplicada; }
    public String getEstadoPago() { return estadoPago; }
    public boolean isConIncidente() { return conIncidente; }

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
