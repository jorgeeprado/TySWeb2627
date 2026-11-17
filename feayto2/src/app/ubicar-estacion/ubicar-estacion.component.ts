import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../services/usuario.service';
import { VehiculoService, EstacionDto, FeatureSimpleDto } from '../services/vehiculo.service';

@Component({
  selector: 'app-ubicar-estacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './ubicar-estacion.component.html',
  styleUrls: ['./ubicar-estacion.component.css']
})
export class UbicarEstacionComponent implements OnInit {
  private router = inject(Router);
  public usuarioService = inject(UsuarioService);
  public vehiculoService = inject(VehiculoService);

  estaciones: EstacionDto[] = [];
  features: FeatureSimpleDto[] = [];

  nombreCalle: string = '';
  numeroCalle: string = '1';
  capacidad: number = 10;
  latitud?: number;
  longitud?: number;
  codigoPostal: string = '';

  guardando: boolean = false;
  cargando: boolean = false;
  cargandoCalles: boolean = false;
  mensaje: string = '';
  error: string = '';

  get usuario() {
    return this.usuarioService.getUsuarioActual();
  }

  ngOnInit(): void {
    this.cargarEstaciones();
    this.cargarFeatures();
  }

  cargarEstaciones(): void {
    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.cargando = true;
    this.vehiculoService.getEstaciones(muni).subscribe({
      next: (est) => {
        this.estaciones = est;
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
      }
    });
  }

  cargarFeatures(): void {
    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.vehiculoService.getFeatures(muni).subscribe({
      next: (f) => {
        this.features = f;
      },
      error: () => {
        this.features = [];
      }
    });
  }

  seleccionarFeature(f: FeatureSimpleDto): void {
    this.nombreCalle = f.name;
    this.numeroCalle = f.number || '1';
    this.latitud = f.latitude;
    this.longitud = f.longitude;
    this.codigoPostal = f.zipCode || '';
  }

  crearEstacion(): void {
    if (!this.nombreCalle || !this.numeroCalle || !this.capacidad) {
      this.error = 'Debes indicar el nombre de la calle, el número y la capacidad.';
      return;
    }

    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.guardando = true;
    this.mensaje = '';
    this.error = '';

    this.vehiculoService.setEstacion({
      municipio: muni,
      name: this.nombreCalle.trim(),
      number: this.numeroCalle.trim(),
      capacidad: this.capacidad,
      latitude: this.latitud,
      longitude: this.longitud,
      zipCode: this.codigoPostal
    }).subscribe({
      next: () => {
        this.guardando = false;
        this.mensaje = `¡Estación "${this.nombreCalle} ${this.numeroCalle}" ubicada correctamente con capacidad para ${this.capacidad} bicicletas!`;
        this.nombreCalle = '';
        this.numeroCalle = '1';
        this.cargarEstaciones();
      },
      error: (err) => {
        this.guardando = false;
        this.error = err?.error?.message || err?.message || 'Error al ubicar la estación';
      }
    });
  }

  cargarCallesDesdeIDEE(): void {
    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.cargandoCalles = true;
    this.error = '';
    this.mensaje = '';

    this.vehiculoService.loadCalles(muni, 0, 100).subscribe({
      next: () => {
        this.cargandoCalles = false;
        this.mensaje = `Direcciones de ${muni} cargadas correctamente desde IDEE / CartoCiudad (RN-MUN-01).`;
        this.cargarFeatures();
      },
      error: (err) => {
        this.cargandoCalles = false;
        this.error = err?.error?.message || err?.message || 'No se pudieron cargar las calles desde IDEE';
      }
    });
  }

  volver(): void {
    this.router.navigate(['/menu']);
  }
}
