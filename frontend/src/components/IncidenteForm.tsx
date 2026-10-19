import { useState, type FormEvent } from 'react'
import { api } from '../api'

export default function IncidenteForm({ bicicletaId, onEnviado }: { bicicletaId: string; onEnviado?: () => void }) {
  const [abierto, setAbierto] = useState(false)
  const [tipo, setTipo] = useState('Otro')
  const [descripcion, setDescripcion] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const enviar = async (e: FormEvent) => {
    e.preventDefault(); setOcupado(true); setMensaje('')
    try {
      const p = await posicionGPS()
      const r = await api.reportar({ bicicletaId, tipo, descripcion, latitud: p.coords.latitude, longitud: p.coords.longitude })
      setMensaje(`Reporte #${r.id} enviado. El operador de flota fue notificado.`); setDescripcion(''); setAbierto(false); onEnviado?.()
    } catch (e) { setMensaje(e instanceof Error ? e.message : 'No pudimos enviar el reporte. Habilita la ubicación e intenta de nuevo.') }
    finally { setOcupado(false) }
  }
  return <section className="reporte">
    <button className="enlace" onClick={() => setAbierto(!abierto)} aria-expanded={abierto}>{abierto ? 'Cancelar reporte' : '¿Algo no está bien? Reportar incidente'}</button>
    {abierto && <form className="formulario" onSubmit={enviar}>
      <h2>Reportar incidente · {bicicletaId}</h2>
      <label>Tipo de incidente<select value={tipo} onChange={e => setTipo(e.target.value)}>{['Daño grave', 'Candado bloqueado', 'Batería', 'Accidente', 'Otro'].map(t => <option key={t}>{t}</option>)}</select></label>
      <label>Cuéntanos qué pasó<textarea value={descripcion} onChange={e => setDescripcion(e.target.value)} minLength={10} maxLength={2000} required /></label>
      <p className="sub">Adjuntaremos tu ubicación GPS para que el equipo pueda ayudarte.</p>
      <button className="primario" disabled={ocupado}>{ocupado ? 'Enviando…' : 'Enviar reporte'}</button>
    </form>}
    {mensaje && <p className="tarjeta" role="status">{mensaje}</p>}
  </section>
}
export function posicionGPS(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) { reject(new Error('Tu navegador no permite obtener la ubicación.')); return }
    navigator.geolocation.getCurrentPosition(resolve, () => reject(new Error('No pudimos obtener tu ubicación. Activa el permiso GPS e intenta de nuevo.')), { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 })
  })
}
