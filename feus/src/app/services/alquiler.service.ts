import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Alquiler, BicicletaDisponible, Estacion } from '../models/alquiler.model';

@Injectable({
  providedIn: 'root'
})
export class AlquilerService {
  private bealquileresUrl = environment.bealquileresUrl;
  private simuladorUrl = environment.simuladorUrl;

  constructor(private http: HttpClient) {}

  getEstaciones(municipio: string = 'Ciudad Real', lat?: number, lon?: number): Observable<Estacion[]> {
    let params = new HttpParams().set('municipio', municipio);
    if (lat !== undefined && lat !== null) {
      params = params.set('lat', lat.toString());
    }
    if (lon !== undefined && lon !== null) {
      params = params.set('lon', lon.toString());
    }
    return this.http.get<Estacion[]>(`${this.bealquileresUrl}/api/alquileres/estaciones`, { params });
  }

  getBicisDisponiblesEnEstacion(estacionId: number): Observable<BicicletaDisponible[]> {
    return this.http.get<BicicletaDisponible[]>(`${this.bealquileresUrl}/api/alquileres/estacion/${estacionId}/disponibles`);
  }

  alquilar(vehiculoId: number, estacionDestinoId?: number): Observable<Alquiler> {
    const payload: any = { vehiculoId };
    if (estacionDestinoId) {
      payload.estacionDestinoId = estacionDestinoId;
    }
    return this.http.post<Alquiler>(`${this.bealquileresUrl}/api/alquileres/alquilar`, payload);
  }

  getAlquilerActivo(): Observable<Alquiler | null> {
    return this.http.get<Alquiler | null>(`${this.bealquileresUrl}/api/alquileres/activo`);
  }

  dejar(vehiculoId: number, estacionDestinoId?: number): Observable<Alquiler> {
    return this.http.post<Alquiler>(`${this.bealquileresUrl}/api/alquileres/dejar`, {
      vehiculoId,
      estacionDestinoId
    });
  }

  getHistorial(): Observable<Alquiler[]> {
    return this.http.get<Alquiler[]>(`${this.bealquileresUrl}/api/alquileres/historial`);
  }

  getVehiculosEnCirculacion(ciudad: string = 'Ciudad Real'): Observable<any[]> {
    return this.http.get<any[]>(`${this.simuladorUrl}/simulador/getVehiculos/${encodeURIComponent(ciudad)}`);
  }
}
