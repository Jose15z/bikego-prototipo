package com.bikego.integration;
import com.bikego.web.dto.ReciboDto;
public interface NotificadorOperaciones {
    void operador(String mensaje);
    void recibo(String correo, ReciboDto recibo);
}
