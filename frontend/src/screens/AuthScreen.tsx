import MetodoPagoForm from '../components/MetodoPagoForm'
import { useState, type FormEvent } from 'react'
import { api, ApiError, sesion } from '../api'
import type { RegistroPayload, Usuario } from '../types'

type Vista = 'login' | 'registro' | 'verificacion' | 'pago'
const vacio: RegistroPayload = { nombre: '', documento: '', correo: '', telefono: '', fechaNacimiento: '', direccion: '', password: '', aceptaTerminos: false }
export default function AuthScreen({ onAutenticado }: { onAutenticado: (u: Usuario) => void }) {
  const [nuevoUsuario, setNuevoUsuario] = useState<Usuario | null>(null)
  const [vista, setVista] = useState<Vista>('login')
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [mostrar, setMostrar] = useState(false)
  const [registro, setRegistro] = useState(vacio)
  const [confirmacion, setConfirmacion] = useState('')
  const [codigo, setCodigo] = useState('')
  const [codigoDesarrollo, setCodigoDesarrollo] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(false)
  const cambiar = (v: Vista) => { setVista(v); setError(''); setMensaje('') }
  const ejecutar = async (accion: () => Promise<void>) => {
    setCargando(true); setError(''); setMensaje('')
    try { await accion() } catch(e) {
      setError(e instanceof Error ? e.message : 'No fue posible completar la solicitud')
      if(e instanceof ApiError && e.codigo === 'CUENTA_NO_VERIFICADA') setVista('verificacion')
    } finally { setCargando(false) }
  }
  const login = (e: FormEvent) => { e.preventDefault(); ejecutar(async () => { const r = await api.login(correo,password); sesion.guardar(r.token); onAutenticado(r.usuario) }) }
  const registrar = (e: FormEvent) => { e.preventDefault(); if(registro.password !== confirmacion) {setError('Las contraseñas no coinciden.');return}; ejecutar(async () => { const r=await api.registro(registro);setCorreo(r.correo);setPassword(registro.password);setCodigoDesarrollo(r.codigoDesarrollo);setVista('verificacion') }) }
  const verificar = (e: FormEvent) => { e.preventDefault(); ejecutar(async () => {await api.verificar(correo,codigo);const r=await api.login(correo,password);sesion.guardar(r.token);setNuevoUsuario(r.usuario);setVista('pago');setCodigo('');setCodigoDesarrollo(null)}) }
  const campo = (nombre: keyof RegistroPayload, valor: string | boolean) => setRegistro(r => ({...r,[nombre]:valor}))
  return <div className="auth-layout">
    <section className="auth-story"><div className="auth-marca"><img src="/bike.svg" alt="" /><strong>BikeGo<span>.</span></strong></div><span className="auth-pill">ARMENIA SE MUEVE CONTIGO</span><h1>Menos prisa.<br/>Más <em>camino.</em></h1><p>Tu próxima aventura empieza a dos ruedas.<br/>Encuentra, desbloquea y disfruta la ciudad.</p>
      <div className="bike-art" aria-hidden="true"><svg viewBox="0 0 560 290" fill="none"><path d="M20 250H540" stroke="#85a599" strokeDasharray="8 12"/><circle cx="130" cy="175" r="78" fill="#174d3f" stroke="#b9ee77" strokeWidth="10"/><circle cx="430" cy="175" r="78" fill="#174d3f" stroke="#b9ee77" strokeWidth="10"/><circle cx="130" cy="175" r="64" stroke="#80a291"/><circle cx="430" cy="175" r="64" stroke="#80a291"/><path d="M130 175L207 70L278 175H130ZM207 70H380L278 175M380 70L430 175M380 70L365 30H332M207 70L195 43" stroke="#f6f5e9" strokeWidth="11" strokeLinecap="round" strokeLinejoin="round"/><path d="M172 41H223M278 175L300 194H317" stroke="#b9ee77" strokeWidth="10" strokeLinecap="round"/><path d="M239 87H300L266 135H240Z" fill="#b9ee77"/><path d="M272 93L259 111H275L260 129" stroke="#174d3f" strokeWidth="4"/><circle cx="278" cy="175" r="12" fill="#b9ee77"/></svg><span className="art-caption">ELÉCTRICA. COMPARTIDA. TUYA POR UN RATO.</span></div>
      <div className="auth-steps"><span><b>01</b> Encuentra</span><span><b>02</b> Escanea</span><span><b>03</b> Pedalea</span></div><small>Movilidad eléctrica · Armenia, Quindío</small>
    </section>
    <section className="auth-contenedor"><div className="auth-topline">TU CIUDAD, A TU RITMO <span>↗</span></div>
      <div className="auth-panel">
      {(vista === 'login' || vista === 'registro') && <div className="auth-tabs"><button className={vista === 'login' ? 'activo' : ''} disabled={cargando} onClick={() => cambiar('login')}>Iniciar sesión</button><button className={vista === 'registro' ? 'activo' : ''} disabled={cargando} onClick={() => cambiar('registro')}>Crear cuenta</button></div>}
      {vista === 'login' && <form className="auth-tarjeta" onSubmit={login}><p className="eyebrow">QUÉ BUENO VERTE DE NUEVO</p><h1>Tu próxima parada<br/>empieza aquí.</h1><p className="sub">Ingresa a tu cuenta y encuentra tu BikeGo.</p>
        <label>Correo electrónico<input type="email" autoComplete="email" placeholder="tu@correo.com" value={correo} onChange={e => setCorreo(e.target.value)} required /></label>
        <label>Contraseña<div className="password-field"><input type={mostrar?'text':'password'} autoComplete="current-password" placeholder="Tu contraseña" value={password} onChange={e => setPassword(e.target.value)} required /><button type="button" aria-label={mostrar?'Ocultar contraseña':'Mostrar contraseña'} onClick={() => setMostrar(!mostrar)}>{mostrar?'Ocultar':'Mostrar'}</button></div></label>
        {error && <p className="error-texto" role="alert">{error}</p>}{mensaje && <p className="tarjeta ok" role="status">{mensaje}</p>}
        <button className="primario grande" disabled={cargando}>{cargando?'Ingresando…':'Vamos a rodar →'}</button><p className="auth-switch">¿Primera vez por aquí? <button type="button" className="enlace" disabled={cargando} onClick={() => cambiar('registro')}>Crea tu cuenta</button></p>
      </form>}
      {vista === 'registro' && <form className="auth-tarjeta" onSubmit={registrar}><p className="eyebrow">ÚNETE A LA COMUNIDAD</p><h1>Haz espacio<br/>para nuevos caminos.</h1><p className="sub">Crea tu cuenta. Puedes añadir tu tarjeta después desde Cuenta y pagos.</p>
        <div className="registro-grid"><label className="ancho">Nombre completo<input autoComplete="name" value={registro.nombre} onChange={e => campo('nombre',e.target.value)} minLength={3} maxLength={100} required /></label><label>Documento<input value={registro.documento} onChange={e => campo('documento',e.target.value)} pattern="[0-9A-Za-z.-]{5,20}" title="Entre 5 y 20 letras, números, puntos o guiones" required /></label><label>Teléfono<input type="tel" autoComplete="tel" pattern="\+?[0-9 ]{7,16}" title="Entre 7 y 16 dígitos, con prefijo + opcional" value={registro.telefono} onChange={e => campo('telefono',e.target.value)} required /></label><label className="ancho">Correo electrónico<input type="email" autoComplete="email" value={registro.correo} onChange={e => campo('correo',e.target.value)} required /></label><label>Fecha de nacimiento<input type="date" max={new Date(Date.now()-86400000).toISOString().slice(0,10)} value={registro.fechaNacimiento} onChange={e => campo('fechaNacimiento',e.target.value)} required /></label><label>Dirección<input autoComplete="street-address" maxLength={160} value={registro.direccion} onChange={e => campo('direccion',e.target.value)} required /></label><label>Contraseña<input type="password" autoComplete="new-password" placeholder="Mínimo 10 caracteres" minLength={10} maxLength={128} value={registro.password} onChange={e => campo('password',e.target.value)} required /></label><label>Confirmar contraseña<input type="password" autoComplete="new-password" value={confirmacion} onChange={e => setConfirmacion(e.target.value)} required /></label></div>
        <details className="terminos"><summary>Términos de uso · versión 2026-09</summary><p>Prototipo académico de alquiler de bicicletas. Debes proporcionar datos veraces, cuidar la bicicleta y devolverla en una zona habilitada. El costo incluye tarifa base, minutos iniciados y un recargo después de cuatro horas. Los incidentes pueden someter el cobro a revisión. Los pagos y notificaciones son simulados en este entorno.</p></details>
        <label className="aceptacion"><input type="checkbox" checked={registro.aceptaTerminos} onChange={e => campo('aceptaTerminos',e.target.checked)} required />He leído y acepto los términos de uso.</label>
        {error && <p className="error-texto" role="alert">{error}</p>}<button className="primario grande" disabled={cargando}>{cargando?'Creando cuenta…':'Crear mi cuenta →'}</button>
      </form>}
      {vista === 'verificacion' && <form className="auth-tarjeta" onSubmit={verificar}><p className="eyebrow">UN ÚLTIMO PASO</p><h1>Verifica tu correo.</h1><p className="sub">Ingresa el código de seis dígitos para {correo}. Es válido durante 15 minutos.</p>{codigoDesarrollo && <p className="codigo-dev">Código de demostración: <strong>{codigoDesarrollo}</strong></p>}
        <label>Código de verificación<input className="codigo-input" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} value={codigo} onChange={e => setCodigo(e.target.value)} required /></label>
        {error && <p className="error-texto" role="alert">{error}</p>}{mensaje && <p role="status">{mensaje}</p>}
        <button className="primario grande" disabled={cargando}>{cargando?'Verificando…':'Verificar cuenta →'}</button><button type="button" className="secundario" disabled={cargando} onClick={() => ejecutar(async () => {const r=await api.reenviar(correo,password);setCodigoDesarrollo(r.codigoDesarrollo);setMensaje(r.mensaje)})}>Enviar un nuevo código</button><button type="button" className="enlace" disabled={cargando} onClick={() => cambiar('login')}>Volver a iniciar sesión</button>
      </form>}
      {vista === 'pago' && nuevoUsuario && <div className="auth-tarjeta"><p className="eyebrow">TU CUENTA YA ESTÁ ACTIVA</p><h1>Prepárate para rodar.</h1><p className="sub">Añade una tarjeta ahora o hazlo antes de tu primer viaje.</p><MetodoPagoForm onGuardado={() => ejecutar(async () => onAutenticado(await api.me()))} /><button className="enlace" onClick={() => onAutenticado(nuevoUsuario)}>Lo haré después · Explorar bicicletas →</button>{error && <p className="error-texto" role="alert">{error}</p>}</div>}
      </div><p className="auth-footer">Cada pedaleada hace una ciudad más amable.</p>
    </section>
  </div>
}
