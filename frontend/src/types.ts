export interface Bicicleta {
  id: string
  bateria: number
  estado: string
  estacion: string
  latitud: number
  longitud: number
  distanciaMetros: number
  autonomiaKm: number
}

export interface Busqueda {
  latitud: number
  longitud: number
  radioMetros: number
  radioMaximoMetros: number
  bicicletas: Bicicleta[]
  mensaje: string | null
  puedeAmpliar: boolean
}

export interface Estacion {
  id: string
  nombre: string
  latitud: number
  longitud: number
}

export interface Usuario {
  id: string
  nombre: string
  estadoCuenta: string
  deudaPendiente: number
  metodoPago: string | null
  bloqueadoHasta: string | null
  escenario: string
}

export interface Viaje {
  idViaje: number
  idUsuario: string
  idBicicleta: string
  horaInicio: string
  estado: string
  segundosTranscurridos: number
  distanciaKm: number
  costoAcumulado: number
  tarifaBase: number
  tarifaMinuto: number
  velocidadPromedioKmh: number
}

export interface Ubicacion {
  lat: number
  lng: number
  origen: 'gps' | 'estacion'
  precisionMetros?: number
  etiqueta: string
}

export interface ErrorApi {
  codigo: string
  mensaje: string
  accionSugerida: string | null
}
