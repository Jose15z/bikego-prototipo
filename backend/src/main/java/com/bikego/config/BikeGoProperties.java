package com.bikego.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de negocio configurables (application.properties, prefijo "bikego").
 * Los valores por defecto salen de los SWR del Plan Vivo de Requisitos v0.3.
 */
@ConfigurationProperties(prefix = "bikego")
public class BikeGoProperties {

    /** SWR-01.2: radio inicial de busqueda en metros. */
    private int radioInicialMetros = 1000;
    /** SWR-01.2: radio maximo al que el usuario puede ampliar. */
    private int radioMaximoMetros = 5000;
    /** RN-005: bateria minima para que una bicicleta este disponible. */
    private int bateriaMinima = 20;
    /** SWR-02.5: tiempo maximo de espera de confirmacion del candado IoT. */
    private long iotTimeoutMs = 5000;
    /** SWR-02.5: reintentos despues del primer intento (2 reintentos = 3 intentos). */
    private int iotReintentos = 2;
    /** SWR-02.7 / RN-008: duracion del bloqueo temporal tras un pago rechazado (parametrizable por el administrador). */
    private int bloqueoPagoMinutos = 30;
    /** Tarifa base del viaje en COP (valor de prueba). */
    private int tarifaBase = 1500;
    /** Tarifa por minuto en COP (valor de prueba). */
    private int tarifaMinuto = 250;
    /** Velocidad promedio usada para estimar la distancia del viaje en el prototipo (km/h). */
    private double velocidadPromedioKmh = 15.0;
    /** Autonomia de una bicicleta con 100 % de bateria (km). */
    private double autonomiaMaximaKm = 60.0;

    public int getRadioInicialMetros() { return radioInicialMetros; }
    public void setRadioInicialMetros(int v) { this.radioInicialMetros = v; }
    public int getRadioMaximoMetros() { return radioMaximoMetros; }
    public void setRadioMaximoMetros(int v) { this.radioMaximoMetros = v; }
    public int getBateriaMinima() { return bateriaMinima; }
    public void setBateriaMinima(int v) { this.bateriaMinima = v; }
    public long getIotTimeoutMs() { return iotTimeoutMs; }
    public void setIotTimeoutMs(long v) { this.iotTimeoutMs = v; }
    public int getIotReintentos() { return iotReintentos; }
    public void setIotReintentos(int v) { this.iotReintentos = v; }
    public int getBloqueoPagoMinutos() { return bloqueoPagoMinutos; }
    public void setBloqueoPagoMinutos(int v) { this.bloqueoPagoMinutos = v; }
    public int getTarifaBase() { return tarifaBase; }
    public void setTarifaBase(int v) { this.tarifaBase = v; }
    public int getTarifaMinuto() { return tarifaMinuto; }
    public void setTarifaMinuto(int v) { this.tarifaMinuto = v; }
    public double getVelocidadPromedioKmh() { return velocidadPromedioKmh; }
    public void setVelocidadPromedioKmh(double v) { this.velocidadPromedioKmh = v; }
    public double getAutonomiaMaximaKm() { return autonomiaMaximaKm; }
    public void setAutonomiaMaximaKm(double v) { this.autonomiaMaximaKm = v; }
}
