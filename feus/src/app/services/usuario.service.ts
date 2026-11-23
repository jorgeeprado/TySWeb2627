import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse, Usuario } from '../models/usuario.model';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private baseUrl = environment.beusuUrl;

  constructor(private http: HttpClient) {}

  login(email: string, pwd: string):Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/users/login`, { email, pwd }).pipe(
      tap((res) => {
        const token = res.token || res.sessionToken;
        if (token && typeof window !== 'undefined') {
          sessionStorage.setItem('token_ciudadano', token);
          sessionStorage.setItem('user_email', email);
          if (res.nombre) sessionStorage.setItem('user_name', res.nombre);
          if (res.pagadoAlta !== undefined) sessionStorage.setItem('user_pagadoAlta', String(res.pagadoAlta));
        }
      })
    );
  }

  registro(usuario: any): Observable<any> {
    const payload = {
      ...usuario,
      rol: usuario.rol || 'CIUDADANO'
    };
    return this.http.post<any>(`${this.baseUrl}/users/register`, payload);
  }

  confirmarCuenta(token: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/users/confirm`, { token });
  }

  // Prepay para la fianza / cuota única de 100€ de alta de ciudadano (RN-CIU-02)
  prepayAltaCiudadano(): Observable<{ idTransaccion: string }> {
    return this.http.post<{ idTransaccion: string }>(`${this.baseUrl}/payments/prepayAltaCiudadano`, {});
  }

  // Confirmar pago en Stripe (Anexo 11.3)
  confirmarPago(idTransaccion: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/payments/confirm`, { idTransaccion });
  }

  validateSession(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.baseUrl}/users/validateSession`).pipe(
      tap((user) => {
        if (user && typeof window !== 'undefined') {
          sessionStorage.setItem('user_pagadoAlta', String(user.pagadoAlta));
        }
      })
    );
  }

  getToken(): string | null {
    if (typeof window !== 'undefined') {
      return sessionStorage.getItem('token_ciudadano');
    }
    return null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasPagadoAlta(): boolean {
    if (typeof window !== 'undefined') {
      return sessionStorage.getItem('user_pagadoAlta') === 'true';
    }
    return false;
  }

  logout(): void {
    if (typeof window !== 'undefined') {
      sessionStorage.removeItem('token_ciudadano');
      sessionStorage.removeItem('user_email');
      sessionStorage.removeItem('user_name');
      sessionStorage.removeItem('user_pagadoAlta');
    }
  }
}
