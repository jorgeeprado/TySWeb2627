import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { VehiculoDto } from '../model/VehiculoDto';
import { environment } from '../../environments/environment';

export interface EstacionDto {
  id?: number;
  name: string;
  number: string;
  latitude?: number;
  longitude?: number;
  capacidad: number;
  municipio?: string;
  bicisOcupadas?: number;
  huecosLibres?: number;
  zipCode?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VehiculoService {
  private readonly urlVehiculos = `${environment.bealquileresUrl}/vehiculos`;
  private readonly urlMunicipios = `${environment.bealquileresUrl}/municipios`;

  private http = inject(HttpClient);

  createVehicles(numberOfVehicles: number): Observable<any> {
    return this.http.post(`${this.urlVehiculos}/createVehicles`, numberOfVehicles).pipe(
      timeout(10000)
    );
  }

  getVehiculos(): Observable<VehiculoDto[]> {
    return this.http.get<VehiculoDto[]>(`${this.urlVehiculos}/getVehiculos`).pipe(
      timeout(8000)
    );
  }

  asignarVehiculos(municipio: string, cantidad: number): Observable<any> {
    return this.http.post(`${this.urlVehiculos}/asignarVehiculos`, { municipio, cantidad }).pipe(
      timeout(10000)
    );
  }

  getEstaciones(municipio: string): Observable<EstacionDto[]> {
    return this.http.get<EstacionDto[]>(`${this.urlMunicipios}/getEstaciones?municipio=${encodeURIComponent(municipio)}`).pipe(
      timeout(8000)
    );
  }

  setEstacion(request: { 
    municipio: string; 
    name: string; 
    number: string; 
    capacidad: number;
    latitude?: number;
    longitude?: number;
    zipCode?: string;
  }): Observable<any> {
    return this.http.post(`${this.urlMunicipios}/setEstacion`, request).pipe(
      timeout(10000)
    );
  }

  ubicarEnEstacion(estacionId: number, cantidad: number): Observable<any> {
    return this.http.post(`${this.urlVehiculos}/ubicarEnEstacion`, { estacionId, cantidad }).pipe(
      timeout(10000)
    );
  }

  getNumeroDeDirecciones(municipio: string): Observable<number> {
    return this.http.get<number>(`${this.urlMunicipios}/getNumeroDeDirecciones?municipio=${encodeURIComponent(municipio)}`).pipe(
      timeout(10000)
    );
  }

  loadCalles(municipio: string, offset: number, limit: number): Observable<any> {
    return this.http.post(`${this.urlMunicipios}/load?offset=${offset}&limit=${limit}`, { municipio }).pipe(
      timeout(20000)
    );
  }
}
