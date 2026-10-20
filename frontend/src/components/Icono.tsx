export type NombreIcono = 'mapa' | 'historial' | 'cuenta' | 'flota' | 'rayo' | 'flecha' | 'ubicacion' | 'bicicleta' | 'check'
const trazos: Record<NombreIcono, string[]> = {
  mapa: ['m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Z','M9 3v15M15 6v15'],
  historial: ['M3 11a9 9 0 1 1 2.6 7','M3 4v7h7','M12 7v5l3 2'],
  cuenta: ['M20 20v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2','M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0'],
  flota: ['M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z'],
  rayo: ['m13 2-9 12h7l-1 8 10-13h-7l1-7Z'],
  flecha: ['M4 12h16m-6-6 6 6-6 6'],
  ubicacion: ['M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z','M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0'],
  bicicleta: ['M9 9h6l4 10M9 9l-4 10h8L9 6H6M15 5h3','M8 18a4 4 0 1 1-8 0 4 4 0 0 1 8 0M24 18a4 4 0 1 1-8 0 4 4 0 0 1 8 0'],
  check: ['m5 12 4 4L19 6'],
}
export default function Icono({ nombre, className = '' }: { nombre: NombreIcono; className?: string }) {
  return <svg className={`icono ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{trazos[nombre].map((d, i) => <path d={d} key={i} />)}</svg>
}
