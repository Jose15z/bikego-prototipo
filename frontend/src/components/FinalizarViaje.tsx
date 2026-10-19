import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Estacion, Zona } from '../types'
import { posicionGPS } from './IncidenteForm'

export default function FinalizarViaje({ id, onFinalizado }: { id: number; onFinalizado: (id: number) => void }) {
  const [estaciones, setEstaciones] = useState<Estacion[]>([])
  const [posicion, setPosicion] = useState<{lat: number; lng: number; precision: number} | null>(null)
  const [zonas, setZonas] = useState<Zona[]>([])
  const [error, setError] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [simulacion, setSimulacion] = useState(false)
  useEffect(() => { api.estaciones().then(setEstaciones).catch(e => setError(e.message)) }, [])
  const comprobar = async (p: {lat: number; lng: number; precision: number}) => {
    setError(''); setPosicion(null); setZonas([])
    if (p.precision > 30) { setError('La precisión debe ser de 30 m o menos. Intenta de nuevo al aire libre.'); return }
    const z = await api.zonas(p.lat, p.lng); setPosicion(p); setZonas(z)
  }
  const gps = async () => { setOcupado(true); setSimulacion(false); try { const p = await posicionGPS(); await comprobar({lat: p.coords.latitude, lng: p.coords.longitude, precision: p.coords.accuracy}) } catch(e) { setError(e instanceof Error ? e.message : 'No pudimos obtener tu ubicación') } finally { setOcupado(false) } }
  const finalizar = async () => {
    if (!posicion) return
    setOcupado(true); setError('')
    try {
      // Revalidar una posición GPS real al confirmar para evitar cerrar con una lectura antigua.
      const p = simulacion ? posicion : await posicionGPS().then(p => ({ lat: p.coords.latitude, lng: p.coords.longitude, precision: p.coords.accuracy }))
      await api.finalizar(id, p.lat, p.lng, p.precision)
      try { await api.pagar(id) } catch { /* El recibo conserva el pago pendiente para poder reintentar. */ }
      onFinalizado(id)
    } catch (e) { setError(e instanceof Error ? e.message : 'No pudimos finalizar el viaje') }
    finally { setOcupado(false) }
  }
  return <section className="tarjeta"><h2>¿Llegaste a tu destino?</h2><p className="sub">Estaciona en una zona habilitada. Confirmaremos el cierre del candado antes de terminar tu viaje.</p>
    <button className="secundario" onClick={gps} disabled={ocupado}>Comprobar mi ubicación GPS</button>
    <details><summary>Simulador local de ubicación de la bicicleta</summary><p className="sub">Solo para probar el prototipo sin un dispositivo IoT.</p><label>Estación de devolución<select disabled={ocupado} defaultValue="" onChange={async e => { const est = estaciones.find(s => s.id === e.target.value); if (est) { setSimulacion(true); try { await comprobar({lat: est.latitud, lng: est.longitud, precision: 0}) } catch(e) { setError(e instanceof Error ? e.message : 'Error al consultar zonas') } } }}><option value="" disabled>Elegir estación de prueba</option>{estaciones.map(e => <option value={e.id} key={e.id}>{e.nombre}</option>)}</select></label></details>
    {zonas.length > 0 && <p className={zonas.some(z => z.dentro) ? 'estado-ok' : 'alerta-texto'}>{zonas.some(z => z.dentro) ? `Zona habilitada: ${zonas.find(z => z.dentro)?.nombre}` : `Estás fuera de zona. La más cercana es ${zonas[0].nombre}, a ${zonas[0].distanciaMetros} m.`}</p>}
    {error && <p className="error-texto" role="alert">{error}</p>}
    <button className="primario grande" disabled={ocupado || !posicion || !zonas.some(z => z.dentro)} onClick={finalizar}>{ocupado ? 'Comprobando…' : 'Finalizar viaje y pagar'}</button>
  </section>
}
