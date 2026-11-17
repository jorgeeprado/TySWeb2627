import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
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
  private router = inject(Router);
  public usuarioService = inject(UsuarioService);
  public vehiculoService = inject(VehiculoService);

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

  cargarDatos(): void {
    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.cargando = true;
    this.error = '';

    this.vehiculoService.getVehiculos(muni).subscribe({
      next: (b) => {
        this.bicicletas = b;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.error = 'No se pudieron cargar las bicicletas del municipio.';
      }
    });

    this.vehiculoService.getEstaciones(muni).subscribe({
      next: (e) => {
        this.estaciones = e;
      },
      error: () => {
        this.estaciones = [];
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
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al cambiar estado de la bicicleta.';
      }
    });
  }

  // -------------------------------------------------------------------------
  // RN-BIC-04: Guardar observaciones ("pinchada", "robada", "en taller", etc.)
  // -------------------------------------------------------------------------
  abrirEditarObservaciones(bici: VehiculoDto): void {
    this.biciParaEditarObservaciones = bici;
    this.textoObservaciones = bici.observaciones || '';
  }

  guardarObservaciones(): void {
    if (!this.biciParaEditarObservaciones) return;

    const bici = this.biciParaEditarObservaciones;
    this.vehiculoService.gestionarBicicleta(bici.id, bici.activo, this.textoObservaciones).subscribe({
      next: (actualizada) => {
        bici.observaciones = actualizada.observaciones;
        this.mensaje = `Observaciones actualizadas para #${bici.matricula}.`;
        this.biciParaEditarObservaciones = null;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al guardar observaciones.';
      }
    });
  }

  cancelarEdicion(): void {
    this.biciParaEditarObservaciones = null;
  }

  // -------------------------------------------------------------------------
  // RN-EST-03, RC-CON-02: Ubicar bicicleta en estación respetando capacidad
  // -------------------------------------------------------------------------
  abrirUbicarEnEstacion(bici: VehiculoDto): void {
    this.biciSeleccionadaParaUbicar = bici;
    this.estacionSeleccionadaId = bici.estacionId || (this.estaciones[0]?.id || null);
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
      }
    });
  }

  cancelarUbicacion(): void {
    this.biciSeleccionadaParaUbicar = null;
  }

  volver(): void {
    this.router.navigate(['/menu']);
  }
}
