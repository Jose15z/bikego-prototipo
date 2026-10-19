package com.bikego.web;
import com.bikego.domain.*;
import com.bikego.service.*;
import com.bikego.web.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class OperacionController {
    private final OperacionService operaciones;
    private final AutenticacionService auth;
    private final AlquilerService alquiler;
    private final com.bikego.repository.MensajeRepository mensajes;
    public OperacionController(OperacionService operaciones,AutenticacionService auth,AlquilerService alquiler,com.bikego.repository.MensajeRepository mensajes) {
        this.operaciones=operaciones;this.auth=auth;this.alquiler=alquiler;this.mensajes=mensajes;
    }
    @GetMapping("/mensajes") public List<Mensaje> mensajes(@RequestHeader(value="Authorization",required=false) String token) {
        Usuario u=usuario(token);
        List<Mensaje> lista=new ArrayList<>(mensajes.findByDestinatarioOrderByFechaDesc(u.getCorreo()));
        if("OPERADOR".equals(u.getRol())) lista.addAll(mensajes.findByDestinatarioOrderByFechaDesc("OPERADORES"));
        lista.sort(Comparator.comparing((Mensaje m)->m.fecha).reversed());return lista;
    }
    private Usuario usuario(String token) {return auth.autenticar(token);}
    @GetMapping("/viajes") public List<ViajeDto> historial(@RequestHeader(value="Authorization",required=false) String token) {
        return operaciones.historial(usuario(token).getId(),alquiler);
    }
    @GetMapping("/zonas") public List<OperacionService.Zona> zonas(@RequestParam double lat,@RequestParam double lng) { return operaciones.zonas(lat,lng); }
    @PostMapping("/viajes/{id}/finalizar") public ReciboDto finalizar(@PathVariable Long id,@Valid @RequestBody PosicionRequest r,@RequestHeader(value="Authorization",required=false) String token) {
        return operaciones.finalizar(id,usuario(token).getId(),r);
    }
    @GetMapping("/viajes/{id}/recibo") public ReciboDto recibo(@PathVariable Long id,@RequestHeader(value="Authorization",required=false) String token) {return operaciones.recibo(id,usuario(token).getId());}
    @PostMapping("/viajes/{id}/pago") public ReciboDto pagar(@PathVariable Long id,@RequestHeader(value="Authorization",required=false) String token) {return operaciones.pagar(id,usuario(token).getId());}
    @GetMapping("/eventos") public List<EventoFlota> eventos(@RequestHeader(value="Authorization",required=false) String token) {return operaciones.eventos(usuario(token));}
    @PostMapping("/incidentes") public EventoFlota incidente(@Valid @RequestBody OperacionRequest r,@RequestHeader(value="Authorization",required=false) String token) {return operaciones.crear(usuario(token),r,false);}
    @PostMapping("/mantenimientos") public EventoFlota mantenimiento(@Valid @RequestBody OperacionRequest r,@RequestHeader(value="Authorization",required=false) String token) {return operaciones.crear(usuario(token),r,true);}
    public record Cierre(@Min(0) @Max(100) int bateria) {}
    @PostMapping("/mantenimientos/{id}/cerrar") public EventoFlota cerrar(@PathVariable Long id,@Valid @RequestBody Cierre r,@RequestHeader(value="Authorization",required=false) String token) {return operaciones.cerrarMantenimiento(usuario(token),id,r.bateria());}
    public record Revision(@NotBlank String accion) {}
    @PostMapping("/incidentes/{id}/revision") public ReciboDto revisar(@PathVariable Long id,@Valid @RequestBody Revision r,@RequestHeader(value="Authorization",required=false) String token) {return operaciones.revisar(usuario(token),id,r.accion());}
    @GetMapping("/flota") public List<Map<String,Object>> flota(@RequestHeader(value="Authorization",required=false) String token) {return operaciones.flota(usuario(token));}
}
