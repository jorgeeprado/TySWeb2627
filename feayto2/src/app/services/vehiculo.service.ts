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

export interface FeatureSimpleDto {
  id: string;
  name: string;
  number: string;
  latitude?: number;
  longitude?: number;
  zipCode?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VehiculoService {
  private readonly urlVehiculos = `${environment.bealquileresUrl}/vehiculos`;
  private readonly urlMunicipios = `${environment.bealquileresUrl}/municipios`;

  private http = inject(HttpClient);

  // -------------------------------------------------------------------------
  // RN-COM-04: Precio de referencia de la bicicleta (1200 euros)
  // -------------------------------------------------------------------------
  getPrecioDeBicicleta(): Observable<number> {
    return this.http.get<number>(`${this.urlVehiculos}/getPrecioDeBicicleta`).pipe(
      timeout(8000)
    );
  }

  // -------------------------------------------------------------------------
  // RN-COM-01, RN-COM-02: Prerreserva sincronizada de bicicletas
  // -------------------------------------------------------------------------
  prerreservar(municipio: string, cantidad: number): Observable<VehiculoDto[]> {
    return this.http.post<VehiculoDto[]>(`${this.urlVehiculos}/prerreservar`, {
      municipio,
      cantidad
    }).pipe(timeout(10000));
  }

  // -------------------------------------------------------------------------
  // RN-COM-03: Confirmar compra definitiva de bicicletas
  // -------------------------------------------------------------------------
  confirmarCompra(municipio: string, cantidad: number): Observable<any> {
    return this.http.post(`${this.urlVehiculos}/confirmarCompra`, {
      municipio,
      cantidad
    }).pipe(timeout(10000));
  }

  createVehicles(numberOfVehicles: number): Observable<any> {
    return this.http.post(`${this.urlVehiculos}/createVehicles`, numberOfVehicles).pipe(
      timeout(10000)
    );
  }

  getVehiculos(municipio?: string): Observable<VehiculoDto[]> {
    const url = municipio
      ? `${this.urlVehiculos}/getVehiculos?municipio=${encodeURIComponent(municipio)}`
      : `${this.urlVehiculos}/getVehiculos`;
    return this.http.get<VehiculoDto[]>(url).pipe(
      timeout(8000)
    );
  }

  // -------------------------------------------------------------------------
  // RN-BIC-04: Gestión administrativa (activa/inactiva y observaciones)
  // -------------------------------------------------------------------------
  gestionarBicicleta(id: number, activo?: boolean, observaciones?: string): Observable<VehiculoDto> {
    return this.http.post<VehiculoDto>(`${this.urlVehiculos}/gestionar`, {
      id,
      activo,
      observaciones
    }).pipe(timeout(8000));
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

  // -------------------------------------------------------------------------
  // RN-EST-03, RC-CON-02: Ubicar bicicleta individual en estación
  // -------------------------------------------------------------------------
  ubicarVehiculoEnEstacion(vehiculoId: number, estacionId: number): Observable<any> {
    return this.http.post(`${this.urlVehiculos}/ubicarVehiculoEnEstacion?vehiculoId=${vehiculoId}&estacionId=${estacionId}`, {}).pipe(
      timeout(10000)
    );
  }

  getFeatures(municipio: string): Observable<FeatureSimpleDto[]> {
    return this.http.get<FeatureSimpleDto[]>(`${this.urlMunicipios}/getFeatures?municipio=${encodeURIComponent(municipio)}`).pipe(
      timeout(10000)
    );
  }

  especificarMunicipio(municipio: string): Observable<any> {
    return this.http.post(`${this.urlMunicipios}/especificar`, { municipio }).pipe(
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
