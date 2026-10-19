import { lazy, Suspense, useEffect, useState } from 'react'
import { api, ApiError, sesion } from './api'
import type { Bicicleta, Ubicacion, Usuario, Viaje } from './types'
import AuthScreen from './screens/AuthScreen'
const MapaScreen = lazy(() => import('./screens/MapaScreen'))
import DetalleScreen from './screens/DetalleScreen'
const EscanearScreen = lazy(() => import('./screens/EscanearScreen'))
import ViajeScreen from './screens/ViajeScreen'
import CuentaScreen from './screens/CuentaScreen'
import HistorialScreen, { ReciboScreen } from './screens/HistorialScreen'
import FlotaScreen from './screens/FlotaScreen'

type Pantalla =
  | { nombre: 'mapa' | 'cuenta' | 'historial' | 'flota' }
  | { nombre: 'recibo'; id: number }
  | { nombre: 'detalle'; bicicletaId: string }
  | { nombre: 'escanear'; bicicleta: Bicicleta }
  | { nombre: 'viaje'; viaje: Viaje; recibidoEn: number }

export default function App() {
  const [pantalla, setPantalla] = useState<Pantalla>({ nombre: 'mapa' })
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargandoSesion, setCargandoSesion] = useState(true)
  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(null)
  const [activo, setActivo] = useState<Viaje | null>(null)
  const [aviso, setAviso] = useState('')
  const abrirViaje = (v: Viaje) => { setActivo(v); setPantalla({ nombre: 'viaje', viaje: v, recibidoEn: performance.now() }) }
  const recuperar = async () => {
    if (!sesion.token()) { setCargandoSesion(false); return }
    setCargandoSesion(true); setAviso('')
    try { const u = await api.me(); setUsuario(u); const viajes = await api.historial(); const v = viajes.find(v => v.estado === 'EN_CURSO'); if (v) abrirViaje(v) }
    catch(e) { if (e instanceof ApiError && e.status === 401) sesion.limpiar(); else setAviso('No pudimos conectar con BikeGo. Comprueba la conexión e intenta de nuevo.') }
    finally { setCargandoSesion(false) }
  }
  useEffect(() => { recuperar(); const expirar = () => { sesion.limpiar(); setUsuario(null); setActivo(null); setPantalla({nombre:'mapa'}); setAviso('Tu sesión venció. Inicia sesión de nuevo.'); }; window.addEventListener('sesion-expirada',expirar); return () => window.removeEventListener('sesion-expirada',expirar) }, [])
  const autenticar = (u: Usuario) => { setUsuario(u); setAviso(''); api.historial().then(vs => { const v = vs.find(v => v.estado === 'EN_CURSO'); if(v) abrirViaje(v) }).catch(e => setAviso(e.message)) }
  const actualizarUsuario = () => { api.me().then(setUsuario).catch(e => setAviso(e.message)) }
  const salir = async () => { try { await api.logout(); sesion.limpiar(); setUsuario(null); setActivo(null); setPantalla({ nombre: 'mapa' }) } catch(e) { setAviso(e instanceof Error ? e.message : 'No pudimos cerrar la sesión') } }
  if (cargandoSesion) return <div className="pantalla centro carga"><div className="spinner" /><p>Preparando tu próximo viaje…</p></div>
  if (!usuario) return <><AuthScreen onAutenticado={autenticar} />{aviso && <div className="aviso-global" role="alert">{aviso}{sesion.token() && <button onClick={recuperar}>Reintentar conexión</button>}</div>}</>
  const nav = [{id:'mapa', titulo:'Explorar', icono:'◉'}, {id:'historial', titulo:'Mis viajes', icono:'↗'}, {id:'cuenta', titulo:'Cuenta y pagos', icono:'▤'}, ...(usuario.rol !== 'CLIENTE' ? [{id:'flota', titulo:'Centro de flota', icono:'⚙'}] : [])] as const
  return <div className="app-layout">
    <aside className="sidebar"><button className="marca" onClick={() => setPantalla({nombre:'mapa'})}><img src="/bike.svg" alt="" />BikeGo<span className="marca-punto">.</span></button><p className="sidebar-sub">Muévete a tu manera.</p>
      <nav aria-label="Navegación principal">{nav.map(n => <button key={n.id} className={pantalla.nombre === n.id ? 'nav-item activo' : 'nav-item'} aria-current={pantalla.nombre === n.id ? 'page' : undefined} onClick={() => setPantalla({nombre: n.id as 'mapa'|'historial'|'cuenta'|'flota'})}><span aria-hidden="true">{n.icono}</span>{n.titulo}</button>)}</nav>
      <div className="sidebar-note"><span>MENOS TRÁFICO.<br/>MÁS CAMINO.</span><p>Redescubre Armenia<br/>a dos ruedas.</p><div className="linea-ruta" /></div>
      <div className="sidebar-profile"><span className="avatar">{usuario.nombre.charAt(0)}</span><div><strong>{usuario.nombre.split(' ')[0]}</strong><small>{usuario.rol === 'CLIENTE' ? 'Comunidad BikeGo' : usuario.rol}</small></div><button className="enlace" onClick={salir}>Salir</button></div>
    </aside>
    <div className="telefono"><header className="barra"><span className="ubicacion-ciudad"><span className="punto-verde" /> Armenia, Quindío</span><span className="barra-lema">Un buen día para pedalear.</span><button className="avatar" onClick={() => setPantalla({nombre:'cuenta'})} aria-label="Mi cuenta">{usuario.nombre.charAt(0)}</button></header>
      <main className="contenido" id="contenido"><Suspense fallback={<div className="pantalla centro"><div className="spinner" /><p>Cargando…</p></div>}>
        {aviso && <div className="tarjeta aviso" role="alert">{aviso}<button className="enlace" onClick={() => setAviso('')}>Cerrar aviso</button></div>}
        {activo && pantalla.nombre !== 'viaje' && <button className="banner-viaje" onClick={() => abrirViaje(activo)}><span>● Tienes un viaje en curso · {activo.idBicicleta}</span><strong>Retomar viaje →</strong></button>}
        {pantalla.nombre === 'mapa' && <><section className="bienvenida"><div><p className="eyebrow">LA CIUDAD ES TUYA</p><h1>Hola, {usuario.nombre.split(' ')[0]}<span className="saludo-punto">.</span><br/>¿A dónde vamos hoy?</h1><p>Una bici cerca, un nuevo camino por descubrir.</p></div><div className="bienvenida-sello"><span>100%</span>eléctrica<small>0 emisiones al rodar</small></div></section><MapaScreen ubicacion={ubicacion} onUbicacion={setUbicacion} onSeleccionar={id => setPantalla({nombre:'detalle',bicicletaId:id})} /></>}
        {pantalla.nombre === 'detalle' && <DetalleScreen bicicletaId={pantalla.bicicletaId} ubicacion={ubicacion} onVolver={() => setPantalla({nombre:'mapa'})} onEscanear={b => setPantalla({nombre:'escanear',bicicleta:b})} />}
        {pantalla.nombre === 'escanear' && <EscanearScreen bicicleta={pantalla.bicicleta} onVolver={() => setPantalla({nombre:'mapa'})} onMetodoPagoActualizado={actualizarUsuario} onViajeIniciado={abrirViaje} />}
        {pantalla.nombre === 'viaje' && <ViajeScreen key={pantalla.viaje.idViaje} viajeInicial={pantalla.viaje} recibidoEn={pantalla.recibidoEn} onFinalizado={id => {setActivo(null);setPantalla({nombre:'recibo',id});actualizarUsuario()}} />}
        {pantalla.nombre === 'cuenta' && <CuentaScreen usuario={usuario} onActualizar={actualizarUsuario} />}
        {pantalla.nombre === 'historial' && <HistorialScreen onViaje={abrirViaje} onRecibo={id => setPantalla({nombre:'recibo',id})} />}
        {pantalla.nombre === 'recibo' && <ReciboScreen id={pantalla.id} onActualizar={actualizarUsuario} />}
        {pantalla.nombre === 'flota' && <FlotaScreen usuario={usuario} />}
      </Suspense></main><footer className="pie"><strong>BikeGo</strong><span>Hecho para recorrer Armenia.</span><span>Prototipo académico · U. del Quindío</span></footer>
    </div>
  </div>
}
