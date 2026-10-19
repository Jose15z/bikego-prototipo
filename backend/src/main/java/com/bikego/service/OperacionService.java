package com.bikego.service;

import com.bikego.config.BikeGoProperties;
import com.bikego.domain.*;
import com.bikego.integration.*;
import com.bikego.repository.*;
import com.bikego.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service
@Transactional
public class OperacionService {
    private final ViajeRepository viajes;
    private final UsuarioRepository usuarios;
    private final BicicletaRepository bicicletas;
    private final EstacionRepository estaciones;
    private final EventoFlotaRepository eventos;
    private final RechazoPagoRepository rechazos;
    private final CandadoIotClient candado;
    private final PasarelaPagoClient pasarela;
    private final NotificadorOperaciones notificador;
    private final BikeGoProperties props;
    private final Clock clock;
    public OperacionService(ViajeRepository viajes, UsuarioRepository usuarios, BicicletaRepository bicicletas,
        EstacionRepository estaciones, EventoFlotaRepository eventos, RechazoPagoRepository rechazos,
        CandadoIotClient candado, PasarelaPagoClient pasarela, NotificadorOperaciones notificador, BikeGoProperties props, Clock clock) {
        this.viajes=viajes; this.usuarios=usuarios; this.bicicletas=bicicletas; this.estaciones=estaciones;
        this.eventos=eventos; this.rechazos=rechazos; this.candado=candado; this.pasarela=pasarela;
        this.notificador=notificador; this.props=props; this.clock=clock;
    }
    private LocalDateTime ahora() { return LocalDateTime.now(clock).withNano(0); }
    private NegocioException invalido(String mensaje) { return new NegocioException(CodigoError.DATOS_INVALIDOS, mensaje); }
    private Viaje propio(Long id, String usuario) {
        Viaje v = viajes.bloquear(id).orElseThrow(() -> new NegocioException(CodigoError.VIAJE_NO_ENCONTRADO,"Viaje no encontrado"));
        if (!v.getUsuario().getId().equals(usuario)) throw new NegocioException(CodigoError.VIAJE_NO_ENCONTRADO,"Viaje no encontrado");
        return v;
    }
    public List<ViajeDto> historial(String usuario, AlquilerService alquiler) {
        return viajes.findByUsuarioIdOrderByHoraInicioDesc(usuario).stream().map(v -> alquiler.consultarViaje(v.getIdViaje(),usuario)).toList();
    }
    public record Zona(String id, String nombre, double latitud, double longitud, long distanciaMetros, boolean dentro) {}
    /** Polígonos rectangulares locales de 40 x 40 m alrededor de cada estación; tolerancia de 30 m al borde. */
    public List<Zona> zonas(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat)>90 || Math.abs(lng)>180) throw invalido("Coordenadas inválidas");
        return estaciones.findAll().stream().map(e -> {
            double x = (lng-e.getLongitud())*111320*Math.cos(Math.toRadians(e.getLatitud()));
            double y = (lat-e.getLatitud())*111320;
            double distanciaBorde = Math.hypot(Math.max(0,Math.abs(x)-20), Math.max(0,Math.abs(y)-20));
            return new Zona(e.getId(),e.getNombre(),e.getLatitud(),e.getLongitud(),Math.round(Geo.distanciaMetros(lat,lng,e.getLatitud(),e.getLongitud())),distanciaBorde<=30);
        }).sorted(Comparator.comparingLong(Zona::distanciaMetros)).toList();
    }
    public ReciboDto finalizar(Long id, String usuario, PosicionRequest p) {
        Viaje v = propio(id,usuario);
        if (v.getEstado()==EstadoViaje.FINALIZADO) return ReciboDto.de(v);
        List<Zona> zonas = zonas(p.latitud(),p.longitud());
        Zona zona = zonas.stream().filter(Zona::dentro).findFirst().orElseThrow(() ->
            new NegocioException(CodigoError.FUERA_DE_ZONA, zonas.isEmpty()?"No hay zonas habilitadas":
            "Acércate a " + zonas.get(0).nombre() + ", a " + zonas.get(0).distanciaMetros() + " m"));
        Bicicleta bici = bicicletas.findByIdParaActualizar(v.getBicicleta().getId()).orElseThrow();
        boolean bloqueado=false;
        for(int i=0;i<=props.getIotReintentos();i++) {
            CompletableFuture<Boolean> f = null;
            try { f=candado.enviarBloqueo(bici.getId()); if(Boolean.TRUE.equals(f.get(props.getIotTimeoutMs(),TimeUnit.MILLISECONDS))) {bloqueado=true;break;} }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); break; }
            catch(ExecutionException|TimeoutException e) { /* Reintentar hasta el límite configurado. */ }
            finally { if(f!=null && !f.isDone()) f.cancel(true); }
        }
        if(!bloqueado) {
            notificador.operador("No se pudo bloquear " + bici.getId() + ", viaje " + id);
            throw new NegocioException(CodigoError.BLOQUEO_FALLIDO,"El candado no confirmó el bloqueo. Tu viaje sigue en curso; el operador fue notificado.");
        }
        LocalDateTime fin=ahora();
        long recargo=Duration.between(v.getHoraInicio(),fin).getSeconds()>14400?props.getRecargoLargo():0;
        v.finalizar(fin,p.latitud(),p.longitud(),props.getTarifaBase(),props.getTarifaMinuto(),recargo);
        bici.devolver(estaciones.findById(zona.id()).orElseThrow(),p.latitud(),p.longitud());
        return ReciboDto.de(v);
    }
    public ReciboDto recibo(Long id,String usuario) {
        Viaje v=propio(id,usuario);
        if(v.getEstado()!=EstadoViaje.FINALIZADO) throw invalido("El viaje todavía está en curso");
        return ReciboDto.de(v);
    }
    public ReciboDto pagar(Long id,String usuario) {
        Viaje v=propio(id,usuario);
        if(v.getEstado()!=EstadoViaje.FINALIZADO) throw invalido("Finaliza el viaje antes de pagar");
        if(!"PENDIENTE".equals(v.getEstadoPago())) return ReciboDto.de(v);
        Usuario u=usuarios.bloquear(usuario).orElseThrow();
        ResultadoPago resultado;
        try { resultado=PagoSeguro.ejecutar(() -> pasarela.cobrar(u.getTokenPago(),v.getTotal(),v.getClavePago())); }
        catch(NegocioException e) { registrarDeuda(v,u,"Servicio de pago no disponible"); var r=ReciboDto.de(v);notificador.recibo(u.getCorreo(),r);return r; }
        if(resultado.autorizado()) {
            v.pagado();
            if(v.isDeudaRegistrada()) {u.ajustarDeuda(-v.getTotal());v.deudaRegistrada(false);}
            if(u.getDeudaPendiente()==0) u.bloquearHasta(null);
        } else registrarDeuda(v,u,resultado.getMotivo());
        ReciboDto recibo=ReciboDto.de(v); notificador.recibo(u.getCorreo(),recibo); return recibo;
    }
    private void registrarDeuda(Viaje v,Usuario u,String motivo) {
        if(!v.isDeudaRegistrada()) {u.ajustarDeuda(v.getTotal());v.deudaRegistrada(true);}
        LocalDateTime hasta=ahora().plusMinutes(props.getBloqueoPagoMinutos());
        u.bloquearHasta(hasta);rechazos.save(new RechazoPago(u,motivo,ahora(),hasta));
    }
    public void personal(Usuario u,boolean soloOperador) {
        if(!u.getRol().equals("OPERADOR") && (soloOperador || !u.getRol().equals("TECNICO")))
            throw new NegocioException(CodigoError.SIN_PERMISO,"No tienes permiso para esta operación");
    }
    public List<EventoFlota> eventos(Usuario u) {
        return eventos.findAllByOrderByFechaDesc().stream().filter(e -> !u.getRol().equals("CLIENTE") || e.responsableId.equals(u.getId())).toList();
    }
    public EventoFlota crear(Usuario u,OperacionRequest r,boolean mantenimiento) {
        if(mantenimiento) personal(u,false);
        List<String> tipos=mantenimiento?List.of("Preventivo","Correctivo"):List.of("Daño grave","Candado bloqueado","Batería","Accidente","Otro");
        if(!tipos.contains(r.tipo())) throw invalido("Tipo no válido");
        Bicicleta b=bicicletas.findByIdParaActualizar(r.bicicletaId()).orElseThrow(() -> invalido("Bicicleta no encontrada"));
        if(mantenimiento && (b.getEstado()==EstadoBicicleta.EN_USO || viajes.findAll().stream().anyMatch(v -> v.getBicicleta().getId().equals(b.getId()) && v.getEstado()==EstadoViaje.EN_CURSO)))
            throw invalido("No puedes iniciar mantenimiento durante un viaje");
        if(mantenimiento && eventos.existsByBicicletaIdAndClaseAndEstado(b.getId(),"MANTENIMIENTO","ABIERTO")) throw invalido("Ya existe un mantenimiento abierto");
        EventoFlota e=new EventoFlota(); e.clase=mantenimiento?"MANTENIMIENTO":"INCIDENTE"; e.bicicletaId=b.getId();
        e.responsableId=u.getId(); e.responsable=u.getNombre();e.fecha=ahora();e.tipo=r.tipo();e.descripcion=r.descripcion().trim();
        e.latitud=r.latitud(); e.longitud=r.longitud();
        if(!mantenimiento) viajes.findByUsuarioIdOrderByHoraInicioDesc(u.getId()).stream()
            .filter(v -> v.getEstado()==EstadoViaje.EN_CURSO && v.getBicicleta().getId().equals(b.getId())).findFirst().ifPresent(v -> e.viajeId=v.getIdViaje());
        if(mantenimiento || r.tipo().equals("Daño grave") || r.tipo().equals("Candado bloqueado")) b.mantenimiento();
        eventos.save(e);
        if(!mantenimiento) notificador.operador("Incidente #"+e.id+" | "+e.bicicletaId+" | "+e.tipo+" | "+e.latitud+", "+e.longitud);
        return e;
    }
    public EventoFlota cerrarMantenimiento(Usuario u,Long id,int bateria) {
        personal(u,false);
        EventoFlota e=eventos.findById(id).orElseThrow(() -> invalido("Mantenimiento no encontrado"));
        if(!"MANTENIMIENTO".equals(e.clase)) throw invalido("No es un mantenimiento");
        Bicicleta b=bicicletas.findByIdParaActualizar(e.bicicletaId).orElseThrow();
        if(!"ABIERTO".equals(e.estado)) return e;
        if(bateria<props.getBateriaMinima() || bateria>100) throw invalido("La batería debe estar entre "+props.getBateriaMinima()+" y 100 %");
        b.habilitar(bateria); e.estado="CERRADO";e.cierre=ahora(); return e;
    }
    public ReciboDto revisar(Usuario u,Long incidente,String accion) {
        personal(u,true);
        EventoFlota e=eventos.findById(incidente).orElseThrow(() -> invalido("Incidente no encontrado"));
        if(e.viajeId==null) throw invalido("El incidente no está asociado a un viaje");
        Viaje v=viajes.bloquear(e.viajeId).orElseThrow();
        if("PROCESADO".equals(v.getEstadoPago()) || "ANULADO".equals(v.getEstadoPago())) throw invalido("Este cobro ya fue resuelto");
        if("REVISAR".equals(accion)) v.revisar();
        else {
            if(!v.isConIncidente()) throw invalido("Primero marca el viaje en revisión");
            if(!List.of("ANULAR","COBRAR").contains(accion) || v.getEstado()!=EstadoViaje.FINALIZADO) throw invalido("Finaliza el viaje antes de resolver el cobro");
            if("ANULAR".equals(accion) && v.isDeudaRegistrada()) {Usuario propietario=usuarios.bloquear(v.getUsuario().getId()).orElseThrow();propietario.ajustarDeuda(-v.getTotal());if(propietario.getDeudaPendiente()==0) propietario.bloquearHasta(null);v.deudaRegistrada(false);}
            v.resolver("ANULAR".equals(accion)); e.estado="RESUELTO";e.cierre=ahora();
        }
        return ReciboDto.de(v);
    }
    public List<Map<String,Object>> flota(Usuario u) {
        personal(u,false);
        return bicicletas.findAll().stream().map(b -> Map.<String,Object>of("id",b.getId(),"estado",b.getEstado().name(),"bateria",b.getBateria(),"estacion",b.getEstacion().getNombre(),"latitud",b.getLatitud(),"longitud",b.getLongitud())).toList();
    }
}
