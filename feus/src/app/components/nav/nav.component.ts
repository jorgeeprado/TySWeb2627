import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header class="header">
      <div class="logo-area">
        <span class="logo-icon">🚲</span>
        <span class="logo-text">BiciCiudad <strong>Ciudadanos</strong></span>
      </div>
      <nav class="nav-links">
        <a *ngIf="isLoggedIn()" routerLink="/alquilar" routerLinkActive="active">Préstamo de Bicicletas</a>
        <a *ngIf="isLoggedIn() && !hasPagadoAlta()" routerLink="/pago-alta" routerLinkActive="active" class="btn-alta">Abonar Fianza (100 €)</a>
        <a *ngIf="!isLoggedIn()" routerLink="/login" routerLinkActive="active">Iniciar Sesión</a>
        <a *ngIf="!isLoggedIn()" routerLink="/registro" routerLinkActive="active">Registrarse</a>
        <button *ngIf="isLoggedIn()" (click)="logout()" class="btn-logout">Cerrar Sesión</button>
      </nav>
    </header>
  `,
  styles: [`
    .header {
      background: #1e293b;
      color: white;
      padding: 0.9rem 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
    .logo-area {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      font-size: 1.25rem;
    }
    .logo-icon { font-size: 1.6rem; }
    .nav-links {
      display: flex;
      gap: 1.2rem;
      align-items: center;
    }
    .nav-links a {
      color: #cbd5e1;
      text-decoration: none;
      font-weight: 500;
      transition: color 0.2s;
    }
    .nav-links a:hover, .nav-links a.active {
      color: #38bdf8;
    }
    .btn-alta {
      background: #f59e0b;
      color: #1e293b !important;
      padding: 0.4rem 0.8rem;
      border-radius: 6px;
      font-weight: bold !important;
    }
    .btn-logout {
      background: #ef4444;
      color: white;
      border: none;
      padding: 0.45rem 0.9rem;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 500;
      transition: background 0.2s;
    }
    .btn-logout:hover {
      background: #dc2626;
    }
  `]
})
export class NavComponent {
  constructor(private usuarioService: UsuarioService, private router: Router) {}

  isLoggedIn(): boolean {
    return this.usuarioService.isLoggedIn();
  }

  hasPagadoAlta(): boolean {
    return this.usuarioService.hasPagadoAlta();
  }

  logout(): void {
    this.usuarioService.logout();
    this.router.navigate(['/login']);
  }
}
