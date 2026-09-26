package com.bikego.integration;

/** Puerto hacia la pasarela de pago externa (actor "Pasarela de pago" del Student Book). */
public interface PasarelaPagoClient {

    /** SWR-02.4: consulta si el metodo tokenizado esta vigente y autorizado. */
    ResultadoPago verificar(String tokenPago);

    /** Tokeniza una tarjeta nueva. La pasarela devuelve un token; el numero completo no se guarda. */
    String tokenizar(String numeroTarjeta, String vencimientoMMAA);
}
