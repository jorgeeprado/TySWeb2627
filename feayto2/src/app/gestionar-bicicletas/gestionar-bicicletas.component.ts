import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { UsuarioService } from '../services/usuario.service';
import { VehiculoService, EstacionDto } from '../services/vehiculo.service';
import { VehiculoDto } from '../model/VehiculoDto';

@Component({
  selector: 'app-gestionar-bicicletas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './gestionar-bicicletas.component.html',
  styleUrls: ['./gestionar-bicicletas.component.css']
})
export class GestionarBicicletasComponent implements OnInit {
  public router = inject(Router);
  public usuarioService = inject(UsuarioService);
  public vehiculoService = inject(VehiculoService);
  private cdr = inject(ChangeDetectorRef);

  bicicletas: VehiculoDto[] = [];
  estaciones: EstacionDto[] = [];

  cargando: boolean = false;
  guardando: boolean = false;
  mensaje: string = '';
  error: string = '';

  // Modal / panel de asignación a estación
  biciSeleccionadaParaUbicar: VehiculoDto | null = null;
  estacionSeleccionadaId: number | null = null;

  // Edición rápida de observaciones
  biciParaEditarObservaciones: VehiculoDto | null = null;
  textoObservaciones: string = '';

  get usuario() {
    return this.usuarioService.getUsuarioActual();
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  irAComprar(): void {
    this.router.navigate(['/comprar-bicicletas']);
  }

  cargarDatos(): void {
    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.cargando = true;
    this.error = '';
    this.cdr.markForCheck();

    this.vehiculoService.getVehiculos(muni).pipe(
      finalize(() => {
        this.cargando = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (b) => {
        this.bicicletas = b || [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.message || 'No se pudieron cargar las bicicletas del municipio.';
        this.cdr.markForCheck();
      }
    });

    this.vehiculoService.getEstaciones(muni).subscribe({
      next: (e) => {
        this.estaciones = e || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.estaciones = [];
        this.cdr.markForCheck();
      }
    });
  }

  // -------------------------------------------------------------------------
  // RN-BIC-04: Cambiar estado activa/inactiva
  // -------------------------------------------------------------------------
  conmutarEstado(bici: VehiculoDto): void {
    const nuevoEstado = !bici.activo;
    this.vehiculoService.gestionarBicicleta(bici.id, nuevoEstado, bici.observaciones).subscribe({
      next: (actualizada) => {
        bici.activo = actualizada.activo;
        this.mensaje = `Bicicleta #${bici.matricula} marcada como ${bici.activo ? 'ACTIVA' : 'INACTIVA'}.`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al cambiar estado de la bicicleta.';
        this.cdr.markForCheck();
      }
    });
  }

  // -------------------------------------------------------------------------
  // RN-BIC-04: Guardar observaciones ("pinchada", "robada", "en taller", etc.)
  // -------------------------------------------------------------------------
  abrirEditarObservaciones(bici: VehiculoDto): void {
    this.biciParaEditarObservaciones = bici;
    this.textoObservaciones = bici.observaciones || '';
    this.cdr.markForCheck();
  }

  guardarObservaciones(): void {
    if (!this.biciParaEditarObservaciones) return;

    const bici = this.biciParaEditarObservaciones;
    this.vehiculoService.gestionarBicicleta(bici.id, bici.activo, this.textoObservaciones).subscribe({
      next: (actualizada) => {
        bici.observaciones = actualizada.observaciones;
        this.mensaje = `Observaciones actualizadas para #${bici.matricula}.`;
        this.biciParaEditarObservaciones = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al guardar observaciones.';
        this.cdr.markForCheck();
      }
    });
  }

  cancelarEdicion(): void {
    this.biciParaEditarObservaciones = null;
    this.cdr.markForCheck();
  }

  // -------------------------------------------------------------------------
  // RN-EST-03, RC-CON-02: Ubicar bicicleta en estación respetando capacidad
  // -------------------------------------------------------------------------
  abrirUbicarEnEstacion(bici: VehiculoDto): void {
    this.biciSeleccionadaParaUbicar = bici;
    this.estacionSeleccionadaId = bici.estacionId || (this.estaciones[0]?.id || null);
    this.cdr.markForCheck();
  }

  confirmarUbicacion(): void {
    if (!this.biciSeleccionadaParaUbicar || !this.estacionSeleccionadaId) {
      this.error = 'Selecciona una estación de destino válida.';
      return;
    }

    const bici = this.biciSeleccionadaParaUbicar;
    const estId = this.estacionSeleccionadaId;
    this.guardando = true;
    this.error = '';
    this.cdr.markForCheck();

    this.vehiculoService.ubicarVehiculoEnEstacion(bici.id, estId).subscribe({
      next: () => {
        this.guardando = false;
        this.mensaje = `Bicicleta #${bici.matricula} ubicada correctamente en la estación seleccionada.`;
        this.biciSeleccionadaParaUbicar = null;
        this.cargarDatos();
      },
      error: (err) => {
        this.guardando = false;
        this.error = err?.error?.message || err?.message || 'No se pudo ubicar la bicicleta en la estación.';
        this.cdr.markForCheck();
      }
    });
  }

  cancelarUbicacion(): void {
    this.biciSeleccionadaParaUbicar = null;
    this.cdr.markForCheck();
  }

  volver(): void {
    this.router.navigate(['/menu']);
  }
}
