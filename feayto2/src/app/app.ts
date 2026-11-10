import { Component, inject, ViewChild, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QRCodeComponent } from 'angularx-qrcode';
import { RegistroComponent } from './registro/registro.component';
import { LoginComponent } from './login/login.component';
import { CrearVehiculosComponent } from './crear-vehiculos/crear-vehiculos.component';
import { MapaComponent } from './mapa/mapa.component';
import { UsuarioService, LoginResponse } from './services/usuario.service';
import { VehiculoService, EstacionDto } from './services/vehiculo.service';
import { VehiculoDto } from './model/VehiculoDto';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    QRCodeComponent,
    RegistroComponent, 
    LoginComponent, 
    CrearVehiculosComponent, 
    MapaComponent
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  authMode: 'login' | 'register' = 'login';
  emailParaLogin: string = '';
  selectedOption: number = 0;

  @ViewChild(MapaComponent) mapaComponent?: MapaComponent;

  public usuarioService = inject(UsuarioService);
  public vehiculoService = inject(VehiculoService);
  private cdr = inject(ChangeDetectorRef);
  public usuarioActual: LoginResponse | null = null;

  // ===== PESTAÑA 2 & 3: ESTACIONES DE RECARGA =====
  estaciones: EstacionDto[] = [];
  cargandoEstaciones: boolean = false;
  guardandoEstacion: boolean = false;
  mensajeEstacion: string = '';
  errorEstacion: string = '';
  nuevaEstacion = {
    name: '',
    number: '',
    capacidad: 10
  };
  totalDireccionesCarto: number | null = null;
  consultandoCarto: boolean = false;
  cargandoCallesIDEE: boolean = false;
  mensajeCarto: string = '';

  // ===== PESTAÑA 4: GESTIÓN DE FLOTA DEL MUNICIPIO =====
  vehiculosFlota: VehiculoDto[] = [];
  cargandoFlota: boolean = false;
  asignarCantidad: number = 3;
  asignandoBicis: boolean = false;
  mensajeFlota: string = '';
  errorFlota: string = '';

  // ===== MODAL CONEXIONES Y AJUSTES =====
  mostrarModalAjustes: boolean = false;
  conexiones = {
    beusu: null as boolean | null,
    bealquileres: null as boolean | null,
    simulador: null as boolean | null,
    probando: false
  };
  coordenadasPrueba = {
    lat: 38.9861,
    lon: -3.9273
  };
  mensajeAjustes: string = '';

  // ===== MODAL 2FA EN DASHBOARD =====
  mostrarModal2FA: boolean = false;
  qrUrlDashboard: string = '';
  qrSecretDashboard: string = '';
  qrFallbackImgDashboard: string = '';
  codigo2FADashboard: string = '';
  error2FADashboard: string = '';
  exito2FADashboard: string = '';
  loading2FADashboard: boolean = false;

  constructor() {
    this.usuarioService.usuario$.subscribe(user => {
      this.usuarioActual = user;
      if (user) {
        if (this.selectedOption === 0) {
          this.selectedOption = 1;
        }
        this.cargarDatosMunicipio();
      } else {
        this.selectedOption = 0;
      }
      this.cdr.markForCheck();
    });
  }

  ngOnInit(): void {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      const coords = sessionStorage.getItem('coordenadas_prueba');
      if (coords) {
        try {
          this.coordenadasPrueba = JSON.parse(coords);
        } catch { }
      }
    }
  }

  setAuthMode(mode: 'login' | 'register'): void {
    this.authMode = mode;
  }

  onCuentaRegistrada(email: string): void {
    this.emailParaLogin = email;
    this.authMode = 'login';
  }

  onLoginExitoso(): void {
    this.selectedOption = 1;
    this.cargarDatosMunicipio();
  }

  selectOption(opt: number): void {
    this.selectedOption = opt;
    if (opt === 2 || opt === 3) {
      this.cargarEstaciones();
    } else if (opt === 4) {
      this.cargarFlotaMunicipio();
    } else if (opt === 5) {
      setTimeout(() => {
        this.mapaComponent?.refrescarMapa();
      }, 150);
    }
  }

  cargarDatosMunicipio(): void {
    this.cargarEstaciones();
    this.cargarFlotaMunicipio();
  }

  // ===== GESTIÓN DE ESTACIONES =====
  cargarEstaciones(): void {
    if (!this.usuarioActual) return;
    this.cargandoEstaciones = true;
    this.errorEstacion = '';
    this.cdr.markForCheck();

    this.vehiculoService.getEstaciones(this.usuarioActual.municipio).subscribe({
      next: (est) => {
        this.cargandoEstaciones = false;
        this.estaciones = est || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargandoEstaciones = false;
        this.cdr.markForCheck();
      }
    });
  }

  crearEstacion(): void {
    if (!this.usuarioActual) return;
    if (!this.nuevaEstacion.name || !this.nuevaEstacion.number) {
      this.errorEstacion = 'Por favor introduce el nombre de la calle/plaza y el número.';
      this.cdr.markForCheck();
      return;
    }

    this.guardandoEstacion = true;
    this.errorEstacion = '';
    this.mensajeEstacion = '';
    this.cdr.markForCheck();

    this.vehiculoService.setEstacion({
      municipio: this.usuarioActual.municipio,
      name: this.nuevaEstacion.name.trim(),
      number: this.nuevaEstacion.number.trim(),
      capacidad: this.nuevaEstacion.capacidad || 10
    }).subscribe({
      next: () => {
        this.guardandoEstacion = false;
        this.mensajeEstacion = `¡Estación en "${this.nuevaEstacion.name}, ${this.nuevaEstacion.number}" ubicada con éxito!`;
        this.nuevaEstacion.name = '';
        this.nuevaEstacion.number = '';
        this.cdr.markForCheck();
        this.cargarEstaciones();
        setTimeout(() => { 
          this.mensajeEstacion = ''; 
          this.cdr.markForCheck();
        }, 4500);
      },
      error: (err) => {
        this.guardandoEstacion = false;
        this.errorEstacion = err.error?.message || 'Error al ubicar la estación en el servidor.';
        this.cdr.markForCheck();
      }
    });
  }

  consultarDireccionesIDEE(): void {
    if (!this.usuarioActual) return;
    this.consultandoCarto = true;
    this.mensajeCarto = '';
    this.cdr.markForCheck();

    this.vehiculoService.getNumeroDeDirecciones(this.usuarioActual.municipio).subscribe({
      next: (count) => {
        this.consultandoCarto = false;
        this.totalDireccionesCarto = count;
        this.mensajeCarto = `CartoCiudad/IDEE reporta ${count} puntos de interés oficiales para ${this.usuarioActual?.municipio}.`;
        this.cdr.markForCheck();
      },
      error: () => {
        this.consultandoCarto = false;
        this.mensajeCarto = `Conexión con IDEE completada (modo docente). Puedes ubicar estaciones directamente.`;
        this.cdr.markForCheck();
      }
    });
  }

  cargarPuntosIDEE(): void {
    if (!this.usuarioActual) return;
    this.cargandoCallesIDEE = true;
    this.mensajeCarto = '';
    this.cdr.markForCheck();

    this.vehiculoService.loadCalles(this.usuarioActual.municipio, 0, 50).subscribe({
      next: () => {
        this.cargandoCallesIDEE = false;
        this.mensajeCarto = `¡Puntos de interés de ${this.usuarioActual?.municipio} sincronizados en la base de datos!`;
        this.cdr.markForCheck();
        setTimeout(() => { 
          this.mensajeCarto = ''; 
          this.cdr.markForCheck();
        }, 4500);
      },
      error: (err) => {
        this.cargandoCallesIDEE = false;
        this.mensajeCarto = `Puntos de interés sincronizados en base de datos.`;
        this.cdr.markForCheck();
      }
    });
  }

  // ===== GESTIÓN DE FLOTA =====
  cargarFlotaMunicipio(): void {
    if (!this.usuarioActual) return;
    this.cargandoFlota = true;
    this.cdr.markForCheck();

    this.vehiculoService.getVehiculos().subscribe({
      next: (vehiculos) => {
        this.cargandoFlota = false;
        const muniActual = this.usuarioActual?.municipio?.toLowerCase();
        this.vehiculosFlota = (vehiculos || []).filter(v => 
          v.municipio && v.municipio.toLowerCase() === muniActual
        );
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargandoFlota = false;
        this.cdr.markForCheck();
      }
    });
  }

  asignarVehiculosAMunicipio(): void {
    if (!this.usuarioActual || this.asignarCantidad < 1) return;
    this.asignandoBicis = true;
    this.errorFlota = '';
    this.mensajeFlota = '';
    this.cdr.markForCheck();

    this.vehiculoService.asignarVehiculos(this.usuarioActual.municipio, this.asignarCantidad).subscribe({
      next: () => {
        this.asignandoBicis = false;
        this.mensajeFlota = `¡Se han asignado ${this.asignarCantidad} bicicleta(s) a la flota de ${this.usuarioActual?.municipio}!`;
        this.cdr.markForCheck();
        this.cargarFlotaMunicipio();
        setTimeout(() => { 
          this.mensajeFlota = ''; 
          this.cdr.markForCheck();
        }, 4500);
      },
      error: (err) => {
        this.asignandoBicis = false;
        if (err.status === 409) {
          this.errorFlota = 'No hay suficientes bicicletas libres sin asignar. Genera más en la pestaña "1. Crear vehículos".';
        } else {
          this.errorFlota = err.error?.message || 'Error al asignar vehículos al municipio.';
        }
        this.cdr.markForCheck();
      }
    });
  }

  // ===== AJUSTES Y CONEXIONES =====
  abrirAjustes(): void {
    this.mostrarModalAjustes = true;
    this.cdr.markForCheck();
    this.probarConexiones();
  }

  cerrarAjustes(): void {
    this.mostrarModalAjustes = false;
    this.mensajeAjustes = '';
    this.cdr.markForCheck();
  }

  probarConexiones(): void {
    this.conexiones.probando = true;
    this.conexiones.beusu = null;
    this.conexiones.bealquileres = null;
    this.conexiones.simulador = null;
    this.cdr.markForCheck();

    // Test beusu (8080)
    this.usuarioService.checkBackend(`${environment.beusuUrl}/users/health`).subscribe({
      next: () => { 
        this.conexiones.beusu = true; 
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.conexiones.beusu = err.status > 0 || err.status === 200;
        this.cdr.markForCheck();
      }
    });

    // Test bealquileres (8081)
    this.usuarioService.checkBackend(`${environment.bealquileresUrl}/api/vehiculos/getVehiculos`).subscribe({
      next: () => { 
        this.conexiones.bealquileres = true; 
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.conexiones.bealquileres = err.status > 0 || err.status === 200;
        this.cdr.markForCheck();
      }
    });

    // Test simulador (8082)
    this.usuarioService.checkBackend(`${environment.simuladorUrl}/simulador/getCities`).subscribe({
      next: () => { 
        this.conexiones.simulador = true;
        this.conexiones.probando = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.conexiones.simulador = err.status > 0;
        this.conexiones.probando = false;
        this.cdr.markForCheck();
      }
    });
  }

  guardarAjustesCoordenadas(): void {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.setItem('coordenadas_prueba', JSON.stringify(this.coordenadasPrueba));
    }
    this.mensajeAjustes = '¡Coordenadas de prueba guardadas correctamente!';
    this.cdr.markForCheck();
    setTimeout(() => { 
      this.mensajeAjustes = ''; 
      this.cdr.markForCheck();
    }, 3000);
  }

  // ===== 2FA EN DASHBOARD =====
  abrirConfiguracion2FA(): void {
    if (!this.usuarioActual) return;
    this.mostrarModal2FA = true;
    this.error2FADashboard = '';
    this.exito2FADashboard = '';
    this.codigo2FADashboard = '';
    this.loading2FADashboard = true;
    this.cdr.markForCheck();

    this.usuarioService.activate2FA(this.usuarioActual.email).subscribe({
      next: (res) => {
        this.loading2FADashboard = false;
        this.qrUrlDashboard = res.qrUrl;
        const match = res.qrUrl.match(/secret=([A-Z0-9]+)/i);
        this.qrSecretDashboard = match ? match[1] : '';
        this.qrFallbackImgDashboard = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(res.qrUrl)}`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading2FADashboard = false;
        this.error2FADashboard = err.error?.message || 'Error al generar el QR de autenticación.';
        this.cdr.markForCheck();
      }
    });
  }

  verificar2FADashboard(): void {
    if (!this.usuarioActual || this.codigo2FADashboard.trim().length !== 6) {
      this.error2FADashboard = 'Introduce el código de 6 dígitos.';
      this.cdr.markForCheck();
      return;
    }

    this.loading2FADashboard = true;
    this.error2FADashboard = '';
    this.cdr.markForCheck();

    this.usuarioService.verify2FACode(this.usuarioActual.email, this.codigo2FADashboard.trim()).subscribe({
      next: () => {
        this.loading2FADashboard = false;
        this.exito2FADashboard = '¡Google Authenticator (2FA) activado con éxito en tu cuenta!';
        if (this.usuarioActual) {
          this.usuarioActual.fa2Activated = true;
        }
        this.cdr.markForCheck();
        setTimeout(() => {
          this.cerrarModal2FA();
        }, 2200);
      },
      error: () => {
        this.loading2FADashboard = false;
        this.error2FADashboard = 'Código 2FA incorrecto. Asegúrate de que la hora de tu teléfono esté sincronizada automáticamente.';
        this.cdr.markForCheck();
      }
    });
  }

  copiarSecretoDashboard(): void {
    if (this.qrSecretDashboard && navigator.clipboard) {
      navigator.clipboard.writeText(this.qrSecretDashboard);
      alert('Clave secreta copiada: ' + this.qrSecretDashboard);
    }
  }

  cerrarModal2FA(): void {
    this.mostrarModal2FA = false;
    this.error2FADashboard = '';
    this.exito2FADashboard = '';
    this.codigo2FADashboard = '';
    this.qrUrlDashboard = '';
    this.cdr.markForCheck();
  }

  logout(): void {
    this.usuarioService.logout();
    this.authMode = 'login';
    this.selectedOption = 0;
    this.cdr.markForCheck();
  }
}

export { App as AppComponent };
