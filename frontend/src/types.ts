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
  referencia?: string
  id: string
  nombre: string
  latitud: number
  longitud: number
}

export interface Usuario {
  id: string
  nombre: string
  correo: string
  estadoCuenta: string
  deudaPendiente: number
  metodoPago: string | null
  bloqueadoHasta: string | null
  rol: string
  escenario: string
}

export interface AuthResponse { token: string; usuario: Usuario }
export interface RegistroResponse { correo: string; mensaje: string; codigoDesarrollo: string | null }
export interface RegistroPayload {
  nombre: string; documento: string; correo: string; telefono: string; fechaNacimiento: string
  direccion: string; password: string; aceptaTerminos: boolean; aceptaPrivacidad: boolean
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
  conIncidente: boolean
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

export interface Recibo {
  idViaje: number; numero: string; bicicleta: string; fecha: string; minutos: number
  tarifaBase: number; tarifaMinuto: number; recargo: number; total: number; estadoPago: string; conIncidente: boolean
}
export interface Zona { id: string; nombre: string; latitud: number; longitud: number; distanciaMetros: number; dentro: boolean }
export interface Evento {
  id: number; clase: string; bicicletaId: string; responsable: string; fecha: string; cierre: string | null
  tipo: string; descripcion: string; estado: string; viajeId: number | null; latitud: number; longitud: number
}
export interface Reporte { bicicletaId: string; tipo: string; descripcion: string; latitud: number; longitud: number }
export interface Rechazo { id: number; motivo: string; fecha: string }

export interface Mensaje { id: number; destinatario: string; canal: string; asunto: string; contenido: string; fecha: string }
