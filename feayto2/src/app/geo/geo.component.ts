import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-geo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './geo.component.html',
  styleUrls: ['./geo.component.css']
})
export class GeoComponent implements OnInit {
  private router = inject(Router);

  // Propiedad oficial vista en la clase de geolocalización
  coordenadas?: GeolocationPosition;

  // Variables para sustituir coordenadas manuales en pruebas docentes (RT-LOC-04)
  latManual?: number;
  lonManual?: number;
  modoManual: boolean = false;
  errorGeo: string = '';

  ngOnInit(): void {
    let self = this;
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          self.coordenadas = pos;
        },
        (err) => {
          self.errorGeo = 'No se pudo obtener la geolocalización real: ' + err.message;
        }
      );

      navigator.geolocation.watchPosition(
        (pos) => {
          if (!self.modoManual) {
            self.coordenadas = pos;
          }
        },
        (err) => {
          console.warn('Error en watchPosition:', err.message);
        }
      );
    } else {
      this.errorGeo = 'El navegador no soporta el API de Geolocalización.';
    }
  }

  aplicarCoordenadasManuales(): void {
    if (this.latManual !== undefined && this.lonManual !== undefined) {
      this.modoManual = true;
      // Mock de GeolocationPosition compatible con la vista
      this.coordenadas = {
        coords: {
          latitude: this.latManual,
          longitude: this.lonManual,
          accuracy: 5,
          altitude: null,
          altitudeAccuracy: null,
          heading: null,
          speed: null
        },
        timestamp: Date.now()
      } as GeolocationPosition;
    }
  }

  restablecerGps(): void {
    this.modoManual = false;
    let self = this;
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((pos) => {
        self.coordenadas = pos;
      });
    }
  }

  volver(): void {
    this.router.navigate(['/menu']);
  }
}
