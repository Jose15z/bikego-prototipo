package com.bikego.integration;

import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Simulador de pasarela en modo sandbox. El resultado depende del token:
 * tok_ok, tok_vencida, tok_fondos, tok_rechazada.
 */
@Component
public class PasarelaPagoSimulada implements PasarelaPagoClient {

    @Override
    public ResultadoPago verificar(String tokenPago) {
        if (tokenPago == null) {
            return ResultadoPago.RECHAZADA;
        }
        if (tokenPago.startsWith("tok_ok")) return ResultadoPago.AUTORIZADO;
        if (tokenPago.startsWith("tok_vencida")) return ResultadoPago.VENCIDA;
        if (tokenPago.startsWith("tok_fondos")) return ResultadoPago.FONDOS_INSUFICIENTES;
        return ResultadoPago.RECHAZADA;
    }

    @Override
    public String tokenizar(String numeroTarjeta, String vencimientoMMAA) {
        String[] partes = vencimientoMMAA.split("/");
        YearMonth vence = YearMonth.of(2000 + Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
        String sufijo = "_" + numeroTarjeta.substring(numeroTarjeta.length() - 4);
        if (vence.isBefore(YearMonth.now())) return "tok_vencida" + sufijo;
        // Tarjeta de prueba terminada en 0002 simula fondos insuficientes (convencion de sandbox).
        if (numeroTarjeta.endsWith("0002")) return "tok_fondos" + sufijo;
        return "tok_ok" + sufijo;
    }
}
