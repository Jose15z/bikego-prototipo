import { validarFormulario } from '../components/validarFormulario'
import BandejaMensajes from '../components/BandejaMensajes'
import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api'
import type { Bicicleta, Evento, Usuario } from '../types'

export default function FlotaScreen({ usuario }: { usuario: Usuario }) {
  const [bicis, setBicis] = useState<Bicicleta[]>([])
  const [eventos, setEventos] = useState<Evento[]>([])
  const [filtro, setFiltro] = useState('')
  const [seleccion, setSeleccion] = useState('')
  const [tipo, setTipo] = useState('Preventivo')
  const [descripcion, setDescripcion] = useState('')
  const [baterias, setBaterias] = useState<Record<number, number>>({})
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const cargar = async () => { const [b, e] = await Promise.all([api.flota(), api.eventos()]); setBicis(b); setEventos(e) }
  useEffect(() => { const refrescar=() => cargar().catch(e => setMensaje(e.message));refrescar();const t=setInterval(refrescar,30000);return () => clearInterval(t) }, [])
  const actuar = async (accion: () => Promise<unknown>, texto: string) => {
    setOcupado(true); setMensaje('')
    try { await accion(); await cargar(); setMensaje(texto) } catch(e) { setMensaje(e instanceof Error ? e.message : 'No fue posible completar la operación') } finally { setOcupado(false) }
  }
  const crear = (e: FormEvent<HTMLFormElement>) => { e.preventDefault(); if(ocupado)return;const invalid=validarFormulario(e,'flota-mensaje');if(invalid){setMensaje(invalid);return}; const b = bicis.find(b => b.id === seleccion); if (b) actuar(() => api.mantenimiento({ bicicletaId: b.id, tipo, descripcion, latitud: b.latitud, longitud: b.longitud }), 'Mantenimiento registrado. La bicicleta ya no está disponible en el mapa.') }
  return <div className="pantalla"><p className="eyebrow">OPERACIÓN BIKEGO</p><h1>Centro de flota</h1><p className="sub">Cuida las bicicletas y acompaña a quienes están en camino.</p>
    {mensaje && <p id="flota-mensaje" className="tarjeta" role="status">{mensaje}</p>}
    <div className="indicadores"><div className="indicador"><strong className="valor">{bicis.length}</strong><span className="rotulo">Bicicletas</span></div><div className="indicador"><strong className="valor">{bicis.filter(b => b.estado === 'EN_MANTENIMIENTO').length}</strong><span className="rotulo">En mantenimiento</span></div><div className="indicador"><strong className="valor">{eventos.filter(e => e.clase === 'INCIDENTE' && e.estado === 'ABIERTO').length}</strong><span className="rotulo">Incidentes abiertos</span></div></div>
    <div className="cuenta-grid"><form noValidate className="formulario" onSubmit={crear}><h2>Registrar mantenimiento</h2><label>Bicicleta<select required value={seleccion} onChange={e => setSeleccion(e.target.value)}><option value="">Selecciona una bicicleta</option>{bicis.map(b => <option value={b.id} key={b.id}>{b.id} · {b.bateria}% · {b.estado.replace(/_/g, ' ')}</option>)}</select></label><label>Tipo<select value={tipo} onChange={e => setTipo(e.target.value)}><option>Preventivo</option><option>Correctivo</option></select></label><label>Trabajo a realizar<textarea className="resize-none" onInput={e=>{e.currentTarget.style.height='auto';e.currentTarget.style.height=e.currentTarget.scrollHeight+'px'}} minLength={10} maxLength={2000} value={descripcion} onChange={e => setDescripcion(e.target.value)} required /></label><button disabled={ocupado} className="primario">Registrar mantenimiento</button></form>
      <section className="tarjeta"><h2>Estado de bicicletas</h2><div className="flota-lista">{bicis.map(b => <div className="actividad" key={b.id}><strong>{b.id}</strong><span>{b.estado.replace(/_/g, ' ')} · {b.bateria}%</span></div>)}</div></section></div>
    <div className="detalle-cabecera"><h2>Historial de operaciones</h2><button className="enlace" disabled={ocupado} onClick={() => actuar(cargar, 'Información actualizada')}>Actualizar</button></div>
    <label>Filtrar historial por bicicleta<select value={filtro} onChange={e => setFiltro(e.target.value)}><option value="">Todas las bicicletas</option>{bicis.map(b => <option key={b.id} value={b.id}>{b.id}</option>)}</select></label>
    {eventos.filter(e => !filtro || e.bicicletaId === filtro).length === 0 && <div className="tarjeta vacio">No hay reportes ni mantenimientos registrados.</div>}
    {eventos.filter(e => !filtro || e.bicicletaId === filtro).map(e => <article key={e.id} className="tarjeta"><div className="detalle-cabecera"><h3>#{e.id} · {e.bicicletaId} · {e.tipo}</h3><span className="estado-ok">{e.estado}</span></div><p>{e.descripcion}</p><p className="sub">{e.responsable} · {new Date(e.fecha).toLocaleString('es-CO')}{e.viajeId ? ` · Viaje #${e.viajeId}` : ''}</p><small className="sub">Ubicación: {e.latitud}, {e.longitud}</small>
      {e.clase === 'MANTENIMIENTO' && e.estado === 'ABIERTO' && <div className="acciones"><label>Batería al entregar (%)<input aria-label={`Batería para mantenimiento ${e.id}`} type="number" min={20} max={100} value={baterias[e.id] ?? 100} onChange={ev => setBaterias({...baterias, [e.id]: Number(ev.target.value)})} /></label><button disabled={ocupado} className="secundario" onClick={() => actuar(() => api.cerrarMantenimiento(e.id, baterias[e.id] ?? 100), 'Mantenimiento cerrado y bicicleta disponible.')}>Cerrar mantenimiento</button></div>}
      {e.clase === 'INCIDENTE' && e.viajeId && e.estado !== 'RESUELTO' && usuario.rol === 'OPERADOR' && <div className="acciones">{[['REVISAR','Revisar cobro'],['ANULAR','Anular cobro'],['COBRAR','Autorizar cobro']].map(([a,t]) => <button disabled={ocupado} key={a} className="secundario" onClick={() => actuar(() => api.revisar(e.id,a), 'Estado del cobro actualizado.')}>{t}</button>)}</div>}
    </article>)}
    <BandejaMensajes />
  </div>
}
