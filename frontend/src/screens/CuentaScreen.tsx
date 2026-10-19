import BandejaMensajes from '../components/BandejaMensajes'
import { useEffect, useState } from 'react'
import { api, formatoCOP } from '../api'
import type { Rechazo, Usuario } from '../types'
import MetodoPagoForm from '../components/MetodoPagoForm'

export default function CuentaScreen({ usuario, onActualizar }: { usuario: Usuario; onActualizar: () => void }) {
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [rechazos, setRechazos] = useState<Rechazo[]>([])
  const cargar = () => api.rechazos().then(setRechazos).catch(e => setMensaje(e.message))
  useEffect(() => { cargar() }, [])
  const validar = async () => {
    setOcupado(true)
    try { await api.validarPago(); setMensaje('Tu método de pago está autorizado para alquilar.') }
    catch (e) { setMensaje(e instanceof Error ? e.message : 'No fue posible validar') }
    finally { setOcupado(false); onActualizar(); cargar() }
  }
  return <div className="pantalla"><div><p className="eyebrow">TU ESPACIO</p><h1>Cuenta y pagos</h1><p className="sub">Todo listo para tu próximo recorrido.</p></div>
    <div className="cuenta-grid"><section className="tarjeta"><div className="avatar grande-avatar">{usuario.nombre.charAt(0)}</div><h2>{usuario.nombre}</h2><p className="sub">{usuario.correo}</p><span className="estado-ok">{usuario.rol === 'CLIENTE' ? 'Ciclista' : usuario.rol} · {usuario.estadoCuenta}</span>
      <hr /><h3>Método de pago</h3><p>{usuario.metodoPago ?? 'Todavía no tienes una tarjeta registrada.'}</p><p className="sub">Saldo pendiente: <strong>{formatoCOP(usuario.deudaPendiente)}</strong></p>
      {usuario.bloqueadoHasta && <p className="alerta-texto">Bloqueo temporal hasta {new Date(usuario.bloqueadoHasta).toLocaleString('es-CO')}</p>}
      <button className="secundario" onClick={validar} disabled={ocupado || !usuario.metodoPago}>{ocupado ? 'Validando…' : 'Validar método de pago'}</button>
    </section><MetodoPagoForm onGuardado={() => { setMensaje('Tarjeta validada y guardada.'); onActualizar(); cargar() }} /></div>
    {mensaje && <p className="tarjeta" role="status">{mensaje}</p>}
    {rechazos.length > 0 && <section className="tarjeta"><h2>Actividad de pagos</h2>{rechazos.map(r => <div key={r.id} className="actividad"><span>{r.motivo}</span><small>{new Date(r.fecha).toLocaleString('es-CO')}</small></div>)}</section>}
    <p className="sub">Entorno de demostración: la pasarela de pagos es simulada y no realiza cargos reales.</p>
    <BandejaMensajes />
  </div>
}
