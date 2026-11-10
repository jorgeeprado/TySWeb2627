import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, timeout } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegistroRequest {
  nombre: string;
  apellidos: string;
  email: string;
  municipio: string;
  latitud: number;
  longitud: number;
  pwd1: string;
  pwd2: string;
}

export interface RegistroResponse {
  message: string;
  email: string;
  token: string;
  confirmUrl: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  nombre: string;
  municipio: string;
  rol: string;
  fa2Activated: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.beusuUrl}/users`;

  private usuarioSubject = new BehaviorSubject<LoginResponse | null>(this.getStoredUser());
  public usuario$ = this.usuarioSubject.asObservable();

  private getStoredUser(): LoginResponse | null {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      const stored = sessionStorage.getItem('usuario_ayto');
      if (stored) {
        try {
          return JSON.parse(stored);
        } catch {
          return null;
        }
      }
    }
    return null;
  }

  registerMunicipio(request: RegistroRequest): Observable<RegistroResponse> {
    return this.http.put<RegistroResponse>(`${this.apiUrl}/registerMunicipio`, request).pipe(
      timeout(10000)
    );
  }

  activarDirecto(email: string, token?: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/activarDirecto`, { email, token }).pipe(
      timeout(8000)
    );
  }

  login(credentials: { email: string; pwd: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      timeout(8000),
      tap(res => {
        if (!res.fa2Activated) {
          this.saveSession(res);
        }
      })
    );
  }

  activate2FA(email: string): Observable<{ qrUrl: string }> {
    return this.http.post<{ qrUrl: string }>(`${this.apiUrl}/activate2FA`, { email }).pipe(
      timeout(8000)
    );
  }

  verify2FACode(email: string, code: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/verify2FACode`, { email, code: parseInt(code, 10) }).pipe(
      timeout(8000),
      tap(res => {
        this.saveSession(res);
      })
    );
  }

  saveSession(res: LoginResponse): void {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.setItem('usuario_ayto', JSON.stringify(res));
      sessionStorage.setItem('token_ayto', res.token);
    }
    this.usuarioSubject.next(res);
  }

  logout(): void {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.removeItem('usuario_ayto');
      sessionStorage.removeItem('token_ayto');
    }
    this.usuarioSubject.next(null);
  }

  getUsuarioActual(): LoginResponse | null {
    return this.usuarioSubject.value;
  }

  checkBackend(url: string): Observable<any> {
    return this.http.get(url, { responseType: 'text' }).pipe(timeout(3500));
  }
}
