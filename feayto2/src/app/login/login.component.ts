import { Component, inject, OnInit, Output, EventEmitter, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QRCodeComponent } from 'angularx-qrcode';
import { UsuarioService, LoginResponse } from '../services/usuario.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, QRCodeComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private cdr = inject(ChangeDetectorRef);

  @Output() loginExitoso = new EventEmitter<void>();
  @Input() email: string = '';

  pwd: string = '';
  loading: boolean = false;
  activando: boolean = false;
  cuentaPendienteActivacion: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  // Flujo 2FA
  paso: 'login' | 'setup2fa' | 'verify2fa' = 'login';
  qrUrl: string = '';
  qrSecret: string = '';
  qrFallbackImg: string = '';
  codigo2FA: string = '';
  emailTemporal: string = '';

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('confirmado') === 'true') {
        this.successMessage = '¡Cuenta activada correctamente! Ya puedes iniciar sesión con tu contraseña.';
        window.history.replaceState({}, '', window.location.pathname);
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    }
  }

  rellenarCredencialesPrueba(emailVal: string = 'test@test.es', pwdVal: string = 'Clave123!'): void {
    this.email = emailVal;
    this.pwd = pwdVal;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  onLogin(): void {
    if (!this.email || !this.pwd) {
      this.errorMessage = 'Por favor, rellena todos los campos.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.loading = true;
    this.cuentaPendienteActivacion = false;
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    this.usuarioService.login({ email: this.email.trim(), pwd: this.pwd }).subscribe({
      next: (user: LoginResponse) => {
        this.loading = false;
        this.emailTemporal = user.email;

        if (user.fa2Activated) {
          this.paso = 'verify2fa';
        } else {
          this.loginExitoso.emit();
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Credenciales incorrectas (correo o contraseña no válidos).';
        } else if (err.status === 403) {
          this.errorMessage = 'La cuenta aún no está activa. Puedes activarla pulsando el botón rápido que aparece abajo.';
          this.cuentaPendienteActivacion = true;
        } else {
          this.errorMessage = err.error?.message || 'Error al conectar con el servidor de autenticación (puerto 8080).';
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  activarMiCuenta(): void {
    if (!this.email) {
      this.errorMessage = 'Introduce tu correo arriba para activar la cuenta.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    this.activando = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    this.usuarioService.activarDirecto(this.email.trim()).subscribe({
      next: () => {
        this.activando = false;
        this.cuentaPendienteActivacion = false;
        this.successMessage = '¡Cuenta activada con éxito! Iniciando sesión...';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
        setTimeout(() => {
          if (this.pwd) {
            this.onLogin();
          }
        }, 1000);
      },
      error: (err) => {
        this.activando = false;
        this.errorMessage = err.error?.message || 'No se pudo activar la cuenta automáticamente.';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  onActivar2FA(): void {
    if (!this.email) {
      this.errorMessage = 'Introduce tu correo institucional arriba para generar el código QR del 2FA.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    this.usuarioService.activate2FA(this.email.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        this.qrUrl = res.qrUrl;
        this.emailTemporal = this.email.trim();
        
        try {
          const match = res.qrUrl.match(/secret=([A-Z0-9]+)/i);
          this.qrSecret = match ? match[1] : '';
        } catch {
          this.qrSecret = '';
        }

        this.qrFallbackImg = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(res.qrUrl)}`;
        this.paso = 'setup2fa';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 404) {
          this.errorMessage = 'No existe ninguna cuenta con ese correo. Regístrate en la pestaña "Registrar Municipio".';
        } else if (err.status === 403) {
          this.errorMessage = 'La cuenta existe pero aún no ha sido activada mediante el enlace de confirmación.';
          this.cuentaPendienteActivacion = true;
        } else {
          this.errorMessage = err.error?.message || 'Error al contactar con el servidor para activar 2FA.';
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  onVerificar2FA(): void {
    if (!this.codigo2FA || this.codigo2FA.trim().length !== 6) {
      this.errorMessage = 'Introduce el código de 6 dígitos que muestra Google Authenticator.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    this.usuarioService.verify2FACode(this.emailTemporal, this.codigo2FA.trim()).subscribe({
      next: () => {
        this.loading = false;
        this.loginExitoso.emit();
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Código 2FA incorrecto. Asegúrate de que la hora de tu teléfono esté sincronizada automáticamente.';
        } else {
          this.errorMessage = err.error?.message || 'Error al verificar el código 2FA.';
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  copiarSecreto(): void {
    if (this.qrSecret && navigator.clipboard) {
      navigator.clipboard.writeText(this.qrSecret);
      alert('Clave secreta copiada al portapapeles: ' + this.qrSecret);
    }
  }

  volverAlLogin(): void {
    this.paso = 'login';
    this.errorMessage = '';
    this.codigo2FA = '';
    this.qrUrl = '';
    this.qrSecret = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }
}
