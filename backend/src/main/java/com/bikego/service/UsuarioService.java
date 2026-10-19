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
    private final com.bikego.repository.RechazoPagoRepository rechazos;
    private final com.bikego.config.BikeGoProperties props;

    public UsuarioService(UsuarioRepository usuarios, PasarelaPagoClient pasarela, Clock clock, com.bikego.repository.RechazoPagoRepository rechazos, com.bikego.config.BikeGoProperties props) {
        this.usuarios = usuarios;
        this.pasarela = pasarela;
        this.clock = clock; this.rechazos = rechazos; this.props = props;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return usuarios.findAll().stream().map(u -> UsuarioDto.de(u, ahora)).toList();
    }

    /** SWR-02.4 flujo alterno 1: actualizar el metodo de pago sin salir del flujo de alquiler. */
    @Transactional(noRollbackFor = NegocioException.class)
    public UsuarioDto actualizarMetodoPago(String usuarioId, MetodoPagoRequest req) {
        Usuario u = usuarios.bloquear(usuarioId)
                .orElseThrow(() -> new NegocioException(CodigoError.USUARIO_NO_ENCONTRADO, "Usuario no encontrado"));
        String numero = req.numeroTarjeta().replaceAll("\\s", "");
        String token = pasarela.tokenizar(numero, req.vencimiento());
        String marca = numero.startsWith("4") ? "Visa" : numero.startsWith("5") ? "Mastercard" : "Tarjeta";
        validarToken(u, token);
        u.actualizarMetodoPago(token, marca, numero.substring(numero.length() - 4));
        u.bloquearHasta(null);
        return UsuarioDto.de(u, LocalDateTime.now(clock));
    }
    @Transactional(noRollbackFor = NegocioException.class)
    public UsuarioDto validar(String id) {
        Usuario u=usuarios.bloquear(id).orElseThrow();
        if(u.getDeudaPendiente()>0) throw new NegocioException(CodigoError.DEUDA_PENDIENTE,"Rechazado: tienes deuda pendiente", "ACTUALIZAR_METODO_PAGO");
        validarToken(u,u.getTokenPago());
        return UsuarioDto.de(u,LocalDateTime.now(clock));
    }
    public java.util.List<java.util.Map<String,Object>> rechazos(String id) {
        return rechazos.findAll().stream().filter(r -> r.getUsuario().getId().equals(id))
            .sorted(java.util.Comparator.comparing(com.bikego.domain.RechazoPago::getFecha).reversed())
            .map(r -> java.util.Map.<String,Object>of("id",r.getId(),"motivo",r.getMotivo(),"fecha",r.getFecha())).toList();
    }
    private void validarToken(Usuario u,String token) {
        var r=PagoSeguro.ejecutar(() -> pasarela.verificar(token));
        if(!r.autorizado()) {
            var ahora=LocalDateTime.now(clock); var hasta=ahora.plusMinutes(props.getBloqueoPagoMinutos());
            u.bloquearHasta(hasta); rechazos.save(new com.bikego.domain.RechazoPago(u,r.getMotivo(),ahora,hasta));
            throw new NegocioException(CodigoError.PAGO_RECHAZADO,r.getMotivo(),"ACTUALIZAR_METODO_PAGO");
        }
    }
}
