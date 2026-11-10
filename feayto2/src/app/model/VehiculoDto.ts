export interface VehiculoDto {
  id : number
  matricula : string
  latitudOrigen : number
  longitudOrigen : number
  nombreOrigen : string
  latitudDestino : number
  longitudDestino : number
  nombreDestino : string
  latitudActual : number
  longitudActual : number
  direccion : string
  kmh : number
  distanciaRecorrida : number
  distanciaTotal : number
  bateria : number
  activo : boolean
  municipio : string
}