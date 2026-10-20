import { useEffect } from 'react'
import L from 'leaflet'
import { Circle, MapContainer, Marker, TileLayer, Tooltip, useMap } from 'react-leaflet'
import type { Bicicleta, Estacion, Ubicacion } from '../types'

interface Props {
  centro: Ubicacion
  radioMetros: number
  bicicletas: Bicicleta[]
  estaciones: Estacion[]
  onSeleccionar: (id: string) => void
}

// Marcadores con divIcon: no dependen de las imagenes por defecto de Leaflet.
const iconoBici = (bateria: number) =>
  L.divIcon({
    className: '',
    html: `<div class="marcador ${bateria < 40 ? 'marcador-bajo' : ''}">${bateria}</div>`,
    iconSize: [34, 34],
    iconAnchor: [17, 17],
  })

const iconoUsuario = L.divIcon({ className: '', html: '<div class="marcador-usuario"></div>', iconSize: [18, 18], iconAnchor: [9, 9] })

const iconoEstacion = L.divIcon({ className: '', html: '<div class="marcador-estacion">E</div>', iconSize: [28, 28], iconAnchor: [14, 14] })

function Recentrar({ lat, lng, radio }: { lat: number; lng: number; radio: number }) {
  const map = useMap()
  useEffect(() => {
    // Un ajuste inmediato evita callbacks de zoom sobre capas que ya se desmontaron.
    map.fitBounds(L.latLng(lat, lng).toBounds(radio * 2), { padding: [8, 8], animate: false })
  }, [lat, lng, radio, map])
  return null
}

function MarcadoresEstaciones({ estaciones }: { estaciones: Estacion[] }) {
  const map = useMap()
  return <>{estaciones.map(e => <Marker key={e.id} position={[e.latitud, e.longitud]} icon={iconoEstacion} zIndexOffset={500} eventHandlers={{click: () => map.setView([e.latitud, e.longitud],18,{animate:false})}}>
    <Tooltip>{e.nombre}{e.referencia ? ` · ${e.referencia}` : ''}<br/>Haz clic para acercarte al acceso.</Tooltip>
  </Marker>)}</>
}

export default function MapaBicicletas({ centro, radioMetros, bicicletas, estaciones, onSeleccionar }: Props) {
  return (
    <div className="mapa">
      <MapContainer center={[centro.lat, centro.lng]} zoom={15} scrollWheelZoom style={{ height: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Recentrar lat={centro.lat} lng={centro.lng} radio={radioMetros} />
        <Circle center={[centro.lat, centro.lng]} radius={radioMetros} pathOptions={{ color: '#0b7a55', weight: 1, fillOpacity: 0.06 }} />
        <Marker position={[centro.lat, centro.lng]} icon={iconoUsuario}>
          <Tooltip>{centro.etiqueta}</Tooltip>
        </Marker>
        <MarcadoresEstaciones estaciones={estaciones} />
        {bicicletas.map((b) => (
          <Marker
            key={b.id}
            position={[b.latitud, b.longitud]}
            icon={iconoBici(b.bateria)}
            eventHandlers={{ click: () => onSeleccionar(b.id) }}
          >
            <Tooltip>
              {b.id} · {b.bateria} %
            </Tooltip>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}
