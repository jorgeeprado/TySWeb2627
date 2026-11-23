import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="registro-container">
      <div class="card">
        <h2>Registro de Ciudadano</h2>
        <p class="subtitle">Crea tu cuenta para acceder a la red municipal de bicicletas</p>

        <div *ngIf="error" class="alert error">{{ error }}</div>
        <div *ngIf="mensaje" class="alert success">{{ mensaje }}</div>

        <form (ngSubmit)="onSubmit()" *ngIf="!registroCompletado">
          <div class="form-row">
            <div class="form-group">
              <label for="nombre">Nombre</label>
              <input type="text" id="nombre" name="nombre" [(ngModel)]="nombre" required placeholder="Tu nombre">
            </div>
            <div class="form-group">
              <label for="apellidos">Apellidos</label>
              <input type="text" id="apellidos" name="apellidos" [(ngModel)]="apellidos" required placeholder="Tus apellidos">
            </div>
          </div>

          <div class="form-group">
            <label for="email">Correo electrónico</label>
            <input type="email" id="email" name="email" [(ngModel)]="email" required placeholder="tu@correo.com">
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="pwd">Contraseña</label>
              <input type="password" id="pwd" name="pwd" [(ngModel)]="pwd" required placeholder="••••••••">
            </div>
            <div class="form-group">
              <label for="pwd2">Repetir contraseña</label>
              <input type="password" id="pwd2" name="pwd2" [(ngModel)]="pwd2" required placeholder="••••••••">
            </div>
          </div>

          <button type="submit" [disabled]="loading" class="btn-submit">
            {{ loading ? 'Registrando...' : 'Registrarme como Ciudadano' }}
          </button>
        </form>

        <div class="footer-links">
          <p>¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .registro-container {
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
      max-width: 500px;
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
    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
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
export class RegistroComponent {
  nombre = '';
  apellidos = '';
  email = '';
  pwd = '';
  pwd2 = '';

  loading = false;
  error: string | null = null;
  mensaje: string | null = null;
  registroCompletado = false;

  constructor(private usuarioService: UsuarioService, private router: Router) {}

  onSubmit(): void {
    if (!this.nombre || !this.apellidos || !this.email || !this.pwd || !this.pwd2) {
      this.error = 'Por favor, completa todos los campos';
      return;
    }

    if (this.pwd !== this.pwd2) {
      this.error = 'Las contraseñas no coinciden';
      return;
    }

    this.loading = true;
    this.error = null;

    const payload = {
      nombre: this.nombre,
      apellidos: this.apellidos,
      email: this.email,
      pwd: this.pwd,
      pwd2: this.pwd2,
      rol: 'CIUDADANO'
    };

    this.usuarioService.registro(payload).subscribe({
      next: (res) => {
        this.loading = false;
        this.registroCompletado = true;
        this.mensaje = '¡Registro completado! Se ha enviado un correo de confirmación (vigencia 24h, RT-CONF-01). Ya puedes iniciar sesión.';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2500);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Error al registrar el usuario';
      }
    });
  }
}
