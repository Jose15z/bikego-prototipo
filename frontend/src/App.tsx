import EnlacesLegales from './components/EnlacesLegales'
import Icono from './components/Icono'
import CreditosFotos from './components/CreditosFotos'
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
const QrPruebaScreen = lazy(() => import('./screens/QrPruebaScreen'))
import FlotaScreen from './screens/FlotaScreen'

type Pantalla =
  | { nombre: 'mapa' | 'cuenta' | 'historial' | 'flota' | 'qr' }
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
    <header className="product-nav"><button className="marca" aria-label="BikeGo, inicio" onClick={() => setPantalla({nombre:'mapa'})}><img src="/bike.svg" alt="" />BikeGo<span className="marca-punto">.</span></button>
      <nav aria-label="Navegación principal">{nav.map(n => <button key={n.id} aria-label={n.titulo} className={pantalla.nombre === n.id ? 'nav-item activo' : 'nav-item'} aria-current={pantalla.nombre === n.id ? 'page' : undefined} onClick={() => setPantalla({nombre:n.id as 'mapa'|'historial'|'cuenta'|'flota'})}><Icono nombre={n.id as 'mapa'|'historial'|'cuenta'|'flota'} /><span>{n.titulo}</span></button>)}</nav>
      <div className="product-profile"><button className="avatar" aria-label="Mi cuenta" onClick={() => setPantalla({nombre:'cuenta'})}>{usuario.nombre.charAt(0)}</button><div><strong>{usuario.nombre.split(' ')[0]}</strong><small>{usuario.rol === 'CLIENTE' ? 'Comunidad BikeGo' : usuario.rol === 'OPERADOR' ? 'Operaciones' : 'Servicio técnico'}</small></div><button className="enlace" onClick={salir}>Salir</button></div>
    </header><div className="telefono">
      <main className="contenido" id="contenido"><Suspense fallback={<div className="pantalla centro"><div className="spinner" /><p>Cargando…</p></div>}>
        {aviso && <div className="tarjeta aviso" role="alert">{aviso}<button className="enlace" onClick={() => setAviso('')}>Cerrar aviso</button></div>}
        {activo && pantalla.nombre !== 'viaje' && <button className="banner-viaje" onClick={() => abrirViaje(activo)}><span>● Tienes un viaje en curso · {activo.idBicicleta}</span><strong>Retomar viaje →</strong></button>}
        {pantalla.nombre === 'mapa' && <><section className="city-hero"><img className="hero-photo" src="/images/armenia-cielos-abiertos.jpg" alt="Calle peatonal de Cielos Abiertos en el centro de Armenia" fetchPriority="high" /><div className="hero-shade" /><div className="hero-copy"><span className="hero-location"><Icono nombre="ubicacion" /> ARMENIA, QUINDÍO</span><h1>Tu ciudad.<br/>Un nuevo <em>camino.</em></h1><p>Cambia la rutina por aire fresco. Encuentra una bici eléctrica y redescubre Armenia a tu ritmo.</p><button className="hero-cta" onClick={() => document.getElementById('estaciones')?.scrollIntoView({behavior:'smooth',block:'start'})}>Encuentra tu bici <Icono nombre="flecha" /></button></div><span className="hero-user"><span /> Hola, {usuario.nombre.split(' ')[0]}</span><span className="hero-caption"><Icono nombre="ubicacion" /> Cielos Abiertos · Centro de Armenia</span></section>
        <div className="ride-benefits"><div><span className="benefit-icon"><Icono nombre="ubicacion" /></span><span><strong>Cerca de ti</strong><small>Elige tu punto de partida</small></span></div><div><span className="benefit-icon"><Icono nombre="rayo" /></span><span><strong>Un impulso eléctrico</strong><small>Menos esfuerzo, más camino</small></span></div><div><span className="benefit-icon"><Icono nombre="bicicleta" /></span><span><strong>A tu ritmo</strong><small>Escanea, desbloquea y pedalea</small></span></div></div>
        <MapaScreen onQr={() => setPantalla({nombre:'qr'})} ubicacion={ubicacion} onUbicacion={setUbicacion} onSeleccionar={id => setPantalla({nombre:'detalle',bicicletaId:id})} />
        <section className="city-invitation"><div><p className="eyebrow">REDESCUBRE LO COTIDIANO</p><h2>La mejor ruta es la que<br/>te dan ganas de recorrer.</h2><p>De la universidad al parque, del centro a tu próxima parada. Hay otra forma de vivir la ciudad.</p></div><Icono nombre="flecha" /></section></>}
        {pantalla.nombre === 'detalle' && <DetalleScreen bicicletaId={pantalla.bicicletaId} ubicacion={ubicacion} onVolver={() => setPantalla({nombre:'mapa'})} onEscanear={b => setPantalla({nombre:'escanear',bicicleta:b})} />}
        {pantalla.nombre === 'escanear' && <EscanearScreen bicicleta={pantalla.bicicleta} onVolver={() => setPantalla({nombre:'mapa'})} onMetodoPagoActualizado={actualizarUsuario} onViajeIniciado={abrirViaje} />}
        {pantalla.nombre === 'viaje' && <ViajeScreen key={pantalla.viaje.idViaje} viajeInicial={pantalla.viaje} recibidoEn={pantalla.recibidoEn} onFinalizado={id => {setActivo(null);setPantalla({nombre:'recibo',id});actualizarUsuario()}} />}
        {pantalla.nombre === 'cuenta' && <CuentaScreen usuario={usuario} onActualizar={actualizarUsuario} />}
        {pantalla.nombre === 'historial' && <HistorialScreen onViaje={abrirViaje} onRecibo={id => setPantalla({nombre:'recibo',id})} />}
        {pantalla.nombre === 'recibo' && <ReciboScreen id={pantalla.id} onActualizar={actualizarUsuario} />}
        {pantalla.nombre === 'qr' && <QrPruebaScreen onVolver={() => setPantalla({nombre:'mapa'})} />}
        {pantalla.nombre === 'flota' && <FlotaScreen usuario={usuario} />}
      </Suspense></main><footer className="pie"><strong>BikeGo</strong><span>Hecho para recorrer Armenia.</span><EnlacesLegales /><CreditosFotos /></footer>
    </div>
  </div>
}
