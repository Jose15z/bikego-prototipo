import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Mensaje } from '../types'
export default function BandejaMensajes() {
  const [mensajes, setMensajes] = useState<Mensaje[]>([])
  const [error, setError] = useState('')
  const cargar = () => { setError(''); api.mensajes().then(setMensajes).catch(e => setError(e.message)) }
  useEffect(() => { cargar(); const t=setInterval(cargar,30000);return () => clearInterval(t) }, [])
  return <section className="tarjeta"><div className="detalle-cabecera"><h2>Notificaciones</h2><button className="enlace" onClick={cargar}>Actualizar</button></div><p className="sub">Aquí encuentras las comunicaciones de tu cuenta y las novedades de tus viajes.</p>{error && <p role="alert">{error}</p>}{mensajes.length === 0 && <p className="sub">No tienes mensajes nuevos.</p>}{mensajes.map(m => <details key={m.id}><summary>{m.asunto} · {new Date(m.fecha).toLocaleString('es-CO')}</summary><pre className="mensaje-contenido">{m.contenido}</pre><small>Para: {m.destinatario} · {m.canal}</small></details>)}</section>
}
