import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="login-container">
      <div class="card">
        <h2>Acceso Ciudadano</h2>
        <p class="subtitle">Inicia sesión para alquilar bicicletas eléctricas</p>

        <div *ngIf="error" class="alert error">{{ error }}</div>
        <div *ngIf="mensaje" class="alert success">{{ mensaje }}</div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="email">Correo electrónico</label>
            <input 
              type="email" 
              id="email" 
              name="email" 
              [(ngModel)]="email" 
              required 
              placeholder="tu@correo.com">
          </div>

          <div class="form-group">
            <label for="pwd">Contraseña</label>
            <input 
              type="password" 
              id="pwd" 
              name="pwd" 
              [(ngModel)]="pwd" 
              required 
              placeholder="••••••••">
          </div>

          <button type="submit" [disabled]="loading" class="btn-submit">
            {{ loading ? 'Iniciando sesión...' : 'Iniciar Sesión' }}
          </button>
        </form>

        <div class="footer-links">
          <p>¿No tienes cuenta? <a routerLink="/registro">Regístrate como ciudadano</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 80px);
      padding: 1.5rem;
    }
    .card {
      background: white;
      padding: 2.2rem;
      border-radius: 12px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.08);
      width: 100%;
      max-width: 420px;
    }
    h2 {
      margin-bottom: 0.4rem;
      color: #0f172a;
      font-size: 1.6rem;
    }
    .subtitle {
      color: #64748b;
      margin-bottom: 1.5rem;
      font-size: 0.95rem;
    }
    .form-group {
      margin-bottom: 1.2rem;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    label {
      font-weight: 500;
      font-size: 0.9rem;
      color: #334155;
    }
    input {
      padding: 0.75rem 0.9rem;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      font-size: 1rem;
      outline: none;
      transition: border-color 0.2s;
    }
    input:focus {
      border-color: #0284c7;
    }
    .btn-submit {
      width: 100%;
      padding: 0.85rem;
      background: #0284c7;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      margin-top: 0.5rem;
      transition: background 0.2s;
    }
    .btn-submit:hover:not(:disabled) {
      background: #0369a1;
    }
    .btn-submit:disabled {
      background: #94a3b8;
      cursor: not-allowed;
    }
    .alert {
      padding: 0.8rem;
      border-radius: 6px;
      margin-bottom: 1.2rem;
      font-size: 0.9rem;
    }
    .alert.error {
      background: #fee2e2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }
    .alert.success {
      background: #dcfce7;
      color: #166534;
      border: 1px solid #bbf7d0;
    }
    .footer-links {
      margin-top: 1.5rem;
      text-align: center;
      font-size: 0.9rem;
      color: #64748b;
    }
    .footer-links a {
      color: #0284c7;
      text-decoration: none;
      font-weight: 500;
    }
  `]
})
export class LoginComponent {
  email = '';
  pwd = '';
  loading = false;
  error: string | null = null;
  mensaje: string | null = null;

  constructor(private usuarioService: UsuarioService, private router: Router) {}

  onSubmit(): void {
    if (!this.email || !this.pwd) {
      this.error = 'Por favor, completa todos los campos';
      return;
    }

    this.loading = true;
    this.error = null;

    this.usuarioService.login(this.email, this.pwd).subscribe({
      next: (res) => {
        this.loading = false;
        // Validar si pagó la cuota de alta
        const pagado = res.pagadoAlta || (res.user && res.user.pagadoAlta);
        if (pagado === true) {
          this.router.navigate(['/alquilar']);
        } else {
          // Consultar validateSession para confirmar
          this.usuarioService.validateSession().subscribe({
            next: (u) => {
              if (u.pagadoAlta) {
                this.router.navigate(['/alquilar']);
              } else {
                this.router.navigate(['/pago-alta']);
              }
            },
            error: () => {
              this.router.navigate(['/pago-alta']);
            }
          });
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 403 || err.status === 401) {
          this.error = err.error?.message || 'Credenciales incorrectas o cuenta no confirmada';
        } else {
          this.error = 'Error de conexión con el servidor de autenticación';
        }
      }
    });
  }
}
