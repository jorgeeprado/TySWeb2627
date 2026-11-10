import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VehiculoService } from '../services/vehiculo.service';
import { VehiculoDto } from '../model/VehiculoDto';

@Component({
  selector: 'app-crear-vehiculos',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './crear-vehiculos.component.html',
  styleUrl: './crear-vehiculos.component.scss'
})
export class CrearVehiculosComponent implements OnInit {

  numberOfVehicles: number = 5;
  precioUnitario: number = 1200; // RN-COM-04: 1.200 € por bicicleta
  vehiculos: VehiculoDto[] = [];
  loading: boolean = false;
  creating: boolean = false;
  successMsg: string = '';
  errorMsg: string = '';

  private service = inject(VehiculoService);
  private cdr = inject(ChangeDetectorRef);

  constructor() {}

  ngOnInit(): void {
    this.getVehiculos();
  }

  get precioTotal(): number {
    return this.numberOfVehicles * this.precioUnitario;
  }

  incrementVehicles(): void {
    if (this.numberOfVehicles < 5) {
      this.numberOfVehicles++;
      this.cdr.markForCheck();
    }
  }

  decrementVehicles(): void {
    if (this.numberOfVehicles > 1) {
      this.numberOfVehicles--;
      this.cdr.markForCheck();
    }
  }

  createVehicles(): void {
    if (this.numberOfVehicles < 1) return;

    this.creating = true;
    this.successMsg = '';
    this.errorMsg = '';
    this.cdr.markForCheck();

    this.service.createVehicles(this.numberOfVehicles).subscribe({
      next: () => {
        this.creating = false;
        this.successMsg = `¡Pedido tramitado con éxito! Se han adquirido ${this.numberOfVehicles} bicicleta(s) eléctricas (Total: ${this.precioTotal} €) con batería al 100%.`;
        this.cdr.markForCheck();
        this.getVehiculos();
        setTimeout(() => {
          this.successMsg = '';
          this.cdr.markForCheck();
        }, 5000);
      },
      error: (error) => {
        this.creating = false;
        this.errorMsg = error?.error?.message || 'Error al tramitar la compra de vehículos en el servidor.';
        this.cdr.markForCheck();
      }
    });
  }

  getVehiculos(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.service.getVehiculos().subscribe({
      next: (ok) => {
        this.loading = false;
        this.vehiculos = ok || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get totalActivos(): number {
    return this.vehiculos.filter(v => v.activo).length;
  }

  get bateriaPromedio(): number {
    if (!this.vehiculos.length) return 0;
    const sum = this.vehiculos.reduce((acc, v) => acc + (v.bateria || 0), 0);
    return Math.round(sum / this.vehiculos.length);
  }
}
