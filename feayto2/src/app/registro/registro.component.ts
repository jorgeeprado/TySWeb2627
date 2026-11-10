import { Component, EventEmitter, Output, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UsuarioService, RegistroResponse } from '../services/usuario.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './registro.component.html',
  styleUrls: ['./registro.component.css']
})
export class RegistroComponent {
  private fb = inject(FormBuilder);
  private usuarioService = inject(UsuarioService);
  private cdr = inject(ChangeDetectorRef);

  @Output() cuentaRegistrada = new EventEmitter<string>();

  registroForm: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';
  loading: boolean = false;
  activandoCuenta: boolean = false;

  ultimoRegistro: { email: string; token: string; confirmUrl: string } | null = null;

  constructor() {
    this.registroForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      apellidos: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      municipio: ['', [Validators.required, Validators.minLength(2)]],
      latitud: [38.9861, [Validators.required, Validators.min(-90), Validators.max(90)]],
      longitud: [-3.9273, [Validators.required, Validators.min(-180), Validators.max(180)]],
      pwd1: ['', [Validators.required, Validators.minLength(6)]],
      pwd2: ['', [Validators.required]]
    });
  }

  get f() {
    return this.registroForm.controls;
  }

  rellenarDatosPrueba(): void {
    this.registroForm.patchValue({
      nombre: 'Jorge',
      apellidos: 'García',
      email: `ayto_${Math.floor(Math.random() * 9000 + 1000)}@ciudadreal.es`,
      municipio: 'Ciudad Real',
      latitud: 38.9861,
      longitud: -3.9273,
      pwd1: 'Clave123!',
      pwd2: 'Clave123!'
    });
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();
  }

  onSubmit(): void {
    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      this.errorMessage = 'Por favor, corrige los campos destacados en rojo antes de continuar.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    if (this.f['pwd1'].value !== this.f['pwd2'].value) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      this.cdr.markForCheck();
      this.cdr.detectChanges();
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.loading = true;
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    const datosForm = this.registroForm.value;

    this.usuarioService.registerMunicipio(datosForm).subscribe({
      next: (res: RegistroResponse) => {
        this.loading = false;
        this.ultimoRegistro = {
          email: datosForm.email,
          token: res?.token || '',
          confirmUrl: res?.confirmUrl || ''
        };
        this.successMessage = `¡Cuenta para ${datosForm.municipio} registrada con éxito! Enlace de confirmación generado.`;
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 404) {
          this.errorMessage = `El municipio "${this.f['municipio'].value}" no existe en el registro oficial (idee.es). Revisa la denominación oficial.`;
        } else if (err.status === 409) {
          this.errorMessage = 'Ya existe una cuenta activa con esta dirección de correo.';
        } else if (err.status === 400) {
          this.errorMessage = 'Datos inválidos. La contraseña debe tener al menos 6 caracteres, incluir una mayúscula, una minúscula, un número y un símbolo (ej: MiClave123!).';
        } else {
          this.errorMessage = err.error?.message || 'Error en el servidor al tramitar el registro o tiempo de espera agotado.';
        }
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  activarCuentaDirecta(): void {
    if (!this.ultimoRegistro) return;

    this.activandoCuenta = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.cdr.detectChanges();

    this.usuarioService.activarDirecto(this.ultimoRegistro.email, this.ultimoRegistro.token).subscribe({
      next: () => {
        this.activandoCuenta = false;
        this.successMessage = '¡Cuenta activada con éxito! Redirigiendo al inicio de sesión...';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
        setTimeout(() => {
          this.irAlLogin();
        }, 1200);
      },
      error: (err) => {
        this.activandoCuenta = false;
        this.errorMessage = err.error?.message || 'No se pudo activar automáticamente la cuenta.';
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      }
    });
  }

  irAlLogin(): void {
    const emailAUsar = this.ultimoRegistro?.email || this.f['email'].value || '';
    this.cuentaRegistrada.emit(emailAUsar);
  }
}
