import { useEffect, useState } from 'react'
import { api, formatoCOP } from '../api'
import type { Recibo, Viaje } from '../types'

export default function HistorialScreen({ onViaje, onRecibo }: { onViaje: (v: Viaje) => void; onRecibo: (id: number) => void }) {
  const [viajes, setViajes] = useState<Viaje[] | null>(null)
  const [error, setError] = useState('')
  const cargar = () => { setError(''); api.historial().then(setViajes).catch(e => setError(e.message)) }
  useEffect(cargar, [])
  return <div className="pantalla"><p className="eyebrow">CADA RECORRIDO CUENTA</p><h1>Mis viajes</h1><p className="sub">Retoma tu viaje o consulta tus recibos.</p>
    {error && <div className="tarjeta error" role="alert">{error}<button className="secundario" onClick={cargar}>Reintentar</button></div>}
    {!viajes && !error && <p>Cargando tus viajes…</p>}
    {viajes?.length === 0 && <div className="tarjeta vacio"><h2>Tu primera aventura te espera</h2><p className="sub">Elige una bicicleta en Explorar. Aquí aparecerán tus recorridos.</p></div>}
    {viajes?.map(v => <button className="viaje-fila" key={v.idViaje} onClick={() => v.estado === 'EN_CURSO' ? onViaje(v) : onRecibo(v.idViaje)}>
      <span className="viaje-icono">↗</span><span><strong>{v.idBicicleta}</strong><small>{new Date(v.horaInicio).toLocaleString('es-CO')}</small></span>
      <span className="viaje-total"><strong>{formatoCOP(v.costoAcumulado)}</strong><small>{v.estado === 'EN_CURSO' ? 'En curso · Retomar' : 'Finalizado · Ver recibo'} →</small></span>
    </button>)}
  </div>
}
export function ReciboScreen({ id, onActualizar }: { id: number; onActualizar: () => void }) {
  const [recibo, setRecibo] = useState<Recibo | null>(null)
  const [error, setError] = useState('')
  const [ocupado, setOcupado] = useState(false)
  useEffect(() => { api.recibo(id).then(setRecibo).catch(e => setError(e.message)) }, [id])
  const pagar = async () => { setOcupado(true); setError(''); try { setRecibo(await api.pagar(id)); onActualizar() } catch (e) { setError(e instanceof Error ? e.message : 'No fue posible procesar el pago') } finally { setOcupado(false) } }
  return <div className="pantalla recibo"><p className="eyebrow">GRACIAS POR RODAR CON BIKEGO</p><h1>Tu viaje, en resumen</h1>
    {error && <p className="error-texto" role="alert">{error}</p>}
    {!recibo && !error && <p>Preparando recibo…</p>}
    {recibo && <section className="tarjeta recibo-papel"><div className="detalle-cabecera"><h2>BikeGo</h2><span className="codigo">{recibo.numero}</span></div><p className="sub">{new Date(recibo.fecha).toLocaleString('es-CO')} · Viaje #{recibo.idViaje} · {recibo.bicicleta}</p>
      <strong className="recibo-total">{formatoCOP(recibo.total)}</strong><span className="estado-ok">{({ PROCESADO: 'Pago procesado', PENDIENTE: 'Pago pendiente', EN_REVISION: 'Cobro en revisión', ANULADO: 'Cobro anulado' } as Record<string,string>)[recibo.estadoPago]}</span>
      <dl className="datos"><dt>Tarifa de desbloqueo</dt><dd>{formatoCOP(recibo.tarifaBase)}</dd><dt>{recibo.minutos} {recibo.minutos === 1 ? 'minuto' : 'minutos'} × {formatoCOP(recibo.tarifaMinuto)}</dt><dd>{formatoCOP(recibo.minutos * recibo.tarifaMinuto)}</dd><dt>Recargo por más de 4 horas</dt><dd>{formatoCOP(recibo.recargo)}</dd><dt>Total del viaje</dt><dd>{formatoCOP(recibo.total)}</dd></dl>
      {recibo.estadoPago === 'PENDIENTE' && <><p className="sub">Si tu tarjeta fue rechazada, actualízala en Cuenta y pagos y vuelve a intentar. Reintentar no duplica el cobro.</p><button className="primario" onClick={pagar} disabled={ocupado}>{ocupado ? 'Procesando…' : 'Pagar viaje'}</button></>}
      {recibo.estadoPago === 'ANULADO' && <p className="sub">El operador anuló el cobro. No tienes que pagar este viaje.</p>}
      <button className="secundario no-imprimir" onClick={() => window.print()}>Imprimir / guardar recibo PDF</button><p className="sub">El recibo está disponible en la bandeja de Cuenta y pagos.</p>
    </section>}
  </div>
}
