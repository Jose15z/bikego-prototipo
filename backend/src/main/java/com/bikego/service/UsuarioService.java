package com.bikego.service;

import com.bikego.domain.Usuario;
import com.bikego.integration.PasarelaPagoClient;
import com.bikego.repository.UsuarioRepository;
import com.bikego.web.dto.MetodoPagoRequest;
import com.bikego.web.dto.UsuarioDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasarelaPagoClient pasarela;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, PasarelaPagoClient pasarela, Clock clock) {
        this.usuarios = usuarios;
        this.pasarela = pasarela;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return usuarios.findAll().stream().map(u -> UsuarioDto.de(u, ahora)).toList();
    }

    /** SWR-02.4 flujo alterno 1: actualizar el metodo de pago sin salir del flujo de alquiler. */
    @Transactional
    public UsuarioDto actualizarMetodoPago(String usuarioId, MetodoPagoRequest req) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NegocioException(CodigoError.USUARIO_NO_ENCONTRADO, "Usuario no encontrado"));
        String numero = req.numeroTarjeta().replaceAll("\\s", "");
        String token = pasarela.tokenizar(numero, req.vencimiento());
        String marca = numero.startsWith("4") ? "Visa" : numero.startsWith("5") ? "Mastercard" : "Tarjeta";
        u.actualizarMetodoPago(token, marca, numero.substring(numero.length() - 4));
        // Decision v0.3 (conflicto SWR-02.4 / SWR-02.7): si la pasarela autoriza el nuevo metodo,
        // se levanta el bloqueo temporal para que el usuario continue el alquiler sin salir del flujo.
        if (pasarela.verificar(token).autorizado()) {
            u.bloquearHasta(null);
        }
        return UsuarioDto.de(u, LocalDateTime.now(clock));
    }
}
