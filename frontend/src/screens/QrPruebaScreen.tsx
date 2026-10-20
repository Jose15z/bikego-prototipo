import CodigoBicicleta from '../components/catalogo/CodigoBicicleta'
import { useEffect, useState } from 'react'

type Catalogo = {
  estaciones: { id: string; nombre: string }[]
  bicicletas: { id: string; estacionId: string; estado: string; bateria: number; candadoConFalla: boolean }[]
}

export default function QrPruebaScreen({ onVolver }: { onVolver: () => void }) {
  const [catalogo, setCatalogo] = useState<Catalogo | null>(null)
  const [filtro, setFiltro] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    fetch('/qr-prueba/catalogo.json').then(r => { if (!r.ok) throw new Error('No pudimos cargar los códigos QR.'); return r.json() }).then(setCatalogo).catch(e => setError(e.message))
  }, [])
  return <div className="pantalla">
    <button className="enlace volver" onClick={onVolver}>← Mapa</button>
    <h1>Códigos de bicicletas</h1>
    <p className="sub">Encuentra el código de tu bicicleta. Descarga la imagen de la bicicleta elegida y úsala en Subir imagen, o muéstrala en otro dispositivo para escanearla.</p>
    <p className="sub">Los códigos identifican cada bicicleta. Consulta su ubicación y disponibilidad actual en Explorar.</p>
    {error && <p role="alert">{error}</p>}
    {!catalogo && !error && <p>Cargando códigos…</p>}
    {catalogo && <>
      <label htmlFor="filtro-qr">Estación de los QR</label>
      <select id="filtro-qr" value={filtro} onChange={e => setFiltro(e.target.value)}><option value="">Todas las estaciones</option>{catalogo.estaciones.map(e => <option key={e.id} value={e.id}>{e.nombre}</option>)}</select>
      {catalogo.estaciones.filter(e => !filtro || e.id === filtro).map(e => <section key={e.id}>
        <h2>{e.nombre}</h2>
        <div className="rejilla-qr">{catalogo.bicicletas.filter(b => b.estacionId === e.id).map(b => <CodigoBicicleta key={b.id} id={b.id} />)}</div>
      </section>)}
    </>}
  </div>
}
