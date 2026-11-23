export interface BicicletaDisponible {
  id: number;
  matricula: string;
  latitud?: number;
  longitud?: number;
  bateria: number;
  estacionId: number;
  estacionNombre: string;
  activo: boolean;
  observaciones?: string;
}

export interface Estacion {
  id: number;
  name: string;
  number?: string;
  latitude: number;
  longitude: number;
  capacidad: number;
  bicicletasDisponibles: number;
  bicicletas?: BicicletaDisponible[];
  distanciaMetros?: number | null;
}

export interface Alquiler {
  id: number;
  usuarioEmail: string;
  vehiculoId: number;
  matricula: string;
  estacionOrigenId: number;
  estacionOrigenNombre: string;
  estacionDestinoId?: number;
  estacionDestinoNombre?: string;
  fechaInicio: string;
  fechaFin?: string;
  activo: boolean;
  distanciaRecorrida: number;
  bateria: number;
}
