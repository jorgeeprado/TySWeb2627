import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { VehiculoDto } from '../model/VehiculoDto';
import { PuntoRutaDto } from '../model/PuntoRutaDto';
import { MunicipioDto } from '../model/MunicipioDto';
import { environment } from '../../environments/environment';
import { GeneracionDto } from '../model/GeneracionDto';

@Injectable({
  providedIn: 'root'
})
export class SimuladorService {

  private readonly urlSimulador = `${environment.simuladorUrl}/simulador`;

  private http = inject(HttpClient);

  getCities() {
    return this.http.get<MunicipioDto[]>(`${this.urlSimulador}/getCities`)
  }

  simulacionAleatoria(city: string) {
    return this.http.post(`${this.urlSimulador}/simulacionAleatoria?city=${city}`, {})
  }

  generateBicycle(city: string) {
    return this.http.post<GeneracionDto>(`${this.urlSimulador}/generateBicycle?city=${city}`, {})
  }

  getVehiculos(city? : string): Observable<VehiculoDto[]> {
    return this.http.get<VehiculoDto[]>(`${this.urlSimulador}/getVehiculos/${city}`);
  }

  getRuta(city : string, matricula: string): Observable<PuntoRutaDto[]> {
    return this.http.get<PuntoRutaDto[]>(
      `${this.urlSimulador}/getRuta/${city}?matricula=${matricula}`
    );
  }

}