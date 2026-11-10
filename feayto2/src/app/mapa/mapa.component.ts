import {
  AfterViewInit,
  Component,
  OnDestroy,
  inject,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';

import { SimuladorService } from '../services/simulador.service';
import { UsuarioService } from '../services/usuario.service';
import { VehiculoService, EstacionDto } from '../services/vehiculo.service';
import { VehiculoDto } from '../model/VehiculoDto';
import { MunicipioDto } from '../model/MunicipioDto';

@Component({
  selector: 'app-mapa',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.scss'
})
export class MapaComponent implements AfterViewInit, OnDestroy {

  cities: MunicipioDto[] = [];
  selectedCity?: MunicipioDto;
  private map!: L.Map;
  private bikeMarkers = new Map<string, L.Marker>();
  private stationMarkers = new Map<number, L.Marker>();
  private rutaSeleccionada?: L.Polyline;
  private marcadorSeleccionTemp?: L.Marker;

  vehiculos: VehiculoDto[] = [];
  estaciones: EstacionDto[] = [];

  // ===== WEBSOCKET (RT-LOC-02) =====
  private ws?: WebSocket;
  wsStatus: 'conectado' | 'conectando' | 'desconectado' = 'desconectado';
  ultimoMensajeWs: string = '';

  // ===== MODO UBICAR ESTACIÓN (CLIC EN MAPA) =====
  modoUbicarEstacion: boolean = false;
  coordenadasSeleccionadas: { lat: number; lng: number } | null = null;
  mostrarModalCrearEstacion: boolean = false;
  nuevaEstacion = {
    name: '',
    number: '',
    capacidad: 10,
    zipCode: '13001'
  };
  guardandoEstacion: boolean = false;

  // ===== MODAL UBICAR BICICLETAS EN ESTACIÓN =====
  mostrarModalUbicarBicis: boolean = false;
  estacionParaBicis: EstacionDto | null = null;
  cantidadBicisUbicar: number = 2;
  ubicandoBicis: boolean = false;

  // ===== MENSAJES Y NOTIFICACIONES =====
  mensajeExito: string = '';
  mensajeError: string = '';

  private vehiculosService = inject(SimuladorService);
  private vehiculoService = inject(VehiculoService);
  private usuarioService = inject(UsuarioService);
  private cdr = inject(ChangeDetectorRef);

  constructor() { }

  ngAfterViewInit(): void {
    const usuario = this.usuarioService.getUsuarioActual();
    const latInicial = 38.9861;
    const lonInicial = -3.9273;

    // Inicializar mapa Leaflet centrado en Ciudad Real
    this.map = L.map('map').setView([latInicial, lonInicial], 15);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    // Evento de clic en el mapa para ubicar estación
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.onMapClick(e);
    });

    // Cargar municipios disponibles
    this.vehiculosService.getCities().subscribe({
      next: (cities) => {
        this.cities = cities || [];
        const userMuni = usuario?.municipio?.toLowerCase();
        let targetCity = this.cities.find(c => c.name.toLowerCase() === userMuni);
        if (!targetCity && this.cities.length > 0) {
          targetCity = this.cities[0];
        }
        if (!targetCity && usuario?.municipio) {
          targetCity = {
            name: usuario.municipio,
            latitude: latInicial,
            longitude: lonInicial
          };
          this.cities.push(targetCity);
        }
        this.cdr.markForCheck();
        if (targetCity) {
          this.loadMap(targetCity);
        }
      },
      error: () => {
        if (usuario?.municipio) {
          const fallbackCity: MunicipioDto = {
            name: usuario.municipio,
            latitude: latInicial,
            longitude: lonInicial
          };
          this.cities = [fallbackCity];
          this.cdr.markForCheck();
          this.loadMap(fallbackCity);
        }
      }
    });
  }

  refrescarMapa(): void {
    if (this.map) {
      setTimeout(() => {
        this.map.invalidateSize();
      }, 100);
    }
  }

  loadMap(city: MunicipioDto) {
    this.selectedCity = city;
    this.map.setView([this.selectedCity.latitude, this.selectedCity.longitude], 15);

    setTimeout(() => {
      this.map.invalidateSize();
    });

    // Cargar estaciones y conectar WebSocket para el municipio
    this.cargarEstaciones();
    this.conectarWebSocket();
    this.loadVehiculos();
    this.cdr.markForCheck();
  }

  // ==========================================
  // WEBSOCKET: ACTUALIZACIÓN INTERACTIVA (RT-LOC-02)
  // ==========================================
  conectarWebSocket(): void {
    const muni = this.selectedCity?.name || this.usuarioService.getUsuarioActual()?.municipio || 'Ciudad Real';

    if (this.ws) {
      try { this.ws.close(); } catch {}
    }

    this.wsStatus = 'conectando';
    this.cdr.markForCheck();

    try {
      this.ws = new WebSocket('ws://localhost:8081/ws');

      this.ws.onopen = () => {
        this.wsStatus = 'conectado';
        const msg = {
          tipo: 'CONSULTAR_VEHICULOS',
          municipio: muni
        };
        this.ws?.send(JSON.stringify(msg));
        this.cdr.markForCheck();
      };

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (Array.isArray(data)) {
            this.vehiculos = data;
            this.actualizarMarcadoresBicis();
            this.ultimoMensajeWs = `Actualizado por WebSocket (${data.length} bicis)`;
          }
        } catch (err) {
          console.warn('Error leyendo mensaje WebSocket:', err);
        }
        this.cdr.markForCheck();
      };

      this.ws.onclose = () => {
        this.wsStatus = 'desconectado';
        this.cdr.markForCheck();
      };

      this.ws.onerror = () => {
        this.wsStatus = 'desconectado';
        this.cdr.markForCheck();
      };
    } catch {
      this.wsStatus = 'desconectado';
      this.cdr.markForCheck();
    }
  }

  recargarWebSocket(): void {
    this.conectarWebSocket();
    this.cargarEstaciones();
    this.loadVehiculos();
    this.mensajeExito = '¡WebSocket reconectado con bealquileres (8081) y datos del mapa actualizados!';
    setTimeout(() => { this.mensajeExito = ''; this.cdr.markForCheck(); }, 4000);
    this.cdr.markForCheck();
  }

  // ==========================================
  // CLIC EN EL MAPA: UBICAR ESTACIÓN
  // ==========================================
  toggleModoUbicarEstacion(): void {
    this.modoUbicarEstacion = !this.modoUbicarEstacion;
    if (this.modoUbicarEstacion) {
      this.mensajeExito = '📍 Modo ubicar estación ACTIVO: Haz clic en cualquier calle o plaza del mapa.';
    } else {
      this.mensajeExito = '';
      if (this.marcadorSeleccionTemp) {
        this.marcadorSeleccionTemp.remove();
        this.marcadorSeleccionTemp = undefined;
      }
    }
    this.cdr.markForCheck();
  }

  onMapClick(e: L.LeafletMouseEvent): void {
    const lat = Number(e.latlng.lat.toFixed(5));
    const lng = Number(e.latlng.lng.toFixed(5));
    this.coordenadasSeleccionadas = { lat, lng };

    // Poner marcador temporal de selección
    if (this.marcadorSeleccionTemp) {
      this.marcadorSeleccionTemp.setLatLng([lat, lng]);
    } else {
      const pinIcon = L.divIcon({
        className: 'temp-pin-marker',
        html: `<div class="temp-pin-pulse">📍</div>`,
        iconSize: [36, 36],
        iconAnchor: [18, 36]
      });
      this.marcadorSeleccionTemp = L.marker([lat, lng], { icon: pinIcon }).addTo(this.map);
    }

    // Sugerir nombre y abrir modal de confirmación
    const numEst = this.estaciones.length + 1;
    this.nuevaEstacion = {
      name: `Estación ${numEst} - ${this.selectedCity?.name || 'Centro'}`,
      number: `Punto #${numEst}`,
      capacidad: 10,
      zipCode: '13001'
    };

    this.mostrarModalCrearEstacion = true;
    this.cdr.markForCheck();
  }

  cancelarCrearEstacion(): void {
    this.mostrarModalCrearEstacion = false;
    if (this.marcadorSeleccionTemp) {
      this.marcadorSeleccionTemp.remove();
      this.marcadorSeleccionTemp = undefined;
    }
    this.cdr.markForCheck();
  }

  guardarEstacionDesdeMapa(): void {
    const muni = this.selectedCity?.name || this.usuarioService.getUsuarioActual()?.municipio || 'Ciudad Real';
    if (!this.coordenadasSeleccionadas || !this.nuevaEstacion.name || !this.nuevaEstacion.number) {
      this.mensajeError = 'Por favor, completa el nombre y el número o calle de la estación.';
      this.cdr.markForCheck();
      return;
    }

    this.guardandoEstacion = true;
    this.mensajeError = '';
    this.cdr.markForCheck();

    this.vehiculoService.setEstacion({
      municipio: muni,
      name: this.nuevaEstacion.name.trim(),
      number: this.nuevaEstacion.number.trim(),
      capacidad: this.nuevaEstacion.capacidad || 10,
      latitude: this.coordenadasSeleccionadas.lat,
      longitude: this.coordenadasSeleccionadas.lng,
      zipCode: this.nuevaEstacion.zipCode.trim()
    }).subscribe({
      next: () => {
        this.guardandoEstacion = false;
        this.mostrarModalCrearEstacion = false;
        this.mensajeExito = `¡Estación "${this.nuevaEstacion.name}" ubicada y creada exitosamente en [${this.coordenadasSeleccionadas?.lat}, ${this.coordenadasSeleccionadas?.lng}]!`;
        if (this.marcadorSeleccionTemp) {
          this.marcadorSeleccionTemp.remove();
          this.marcadorSeleccionTemp = undefined;
        }
        this.modoUbicarEstacion = false;
        this.cargarEstaciones();
        setTimeout(() => { this.mensajeExito = ''; this.cdr.markForCheck(); }, 5000);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.guardandoEstacion = false;
        this.mensajeError = err.error?.message || 'Error al guardar la estación en el servidor.';
        this.cdr.markForCheck();
      }
    });
  }

  // ==========================================
  // CARGA Y DIBUJADO DE ESTACIONES CON BADGE [4/10]
  // ==========================================
  cargarEstaciones(): void {
    const muni = this.selectedCity?.name || this.usuarioService.getUsuarioActual()?.municipio || 'Ciudad Real';
    this.vehiculoService.getEstaciones(muni).subscribe({
      next: (est) => {
        this.estaciones = est || [];
        this.dibujarEstacionesEnMapa();
        this.cdr.markForCheck();
      },
      error: () => {
        this.cdr.markForCheck();
      }
    });
  }

  private crearEstacionIcon(e: EstacionDto): L.DivIcon {
    const ocupadas = e.bicisOcupadas || 0;
    const cap = e.capacidad || 10;
    const badgeClass = ocupadas >= cap ? 'llena' : (ocupadas === 0 ? 'vacia' : 'disponible');

    return L.divIcon({
      className: 'estacion-marker-wrapper',
      html: `
        <div class="estacion-container">
          <div class="estacion-badge-square ${badgeClass}">
            <span class="badge-ratio">${ocupadas}/${cap}</span>
          </div>
          <div class="estacion-pin-icon">⚡</div>
          <div class="estacion-label-name">${e.name}</div>
        </div>
      `,
      iconSize: [80, 60],
      iconAnchor: [40, 48],
      popupAnchor: [0, -48]
    });
  }

  private dibujarEstacionesEnMapa(): void {
    // Eliminar marcadores anteriores
    for (const marker of this.stationMarkers.values()) {
      marker.remove();
    }
    this.stationMarkers.clear();

    for (const est of this.estaciones) {
      if (est.latitude == null || est.longitude == null) continue;

      const marker = L.marker([est.latitude, est.longitude], {
        icon: this.crearEstacionIcon(est)
      });

      const ocupadas = est.bicisOcupadas || 0;
      const libres = est.huecosLibres != null ? est.huecosLibres : Math.max(0, est.capacidad - ocupadas);
      const cp = est.zipCode || est.number || '13001';

      const popupContent = `
        <div class="estacion-popup-card">
          <div class="popup-title">
            <span class="popup-icon">⚡</span>
            <strong>${est.name}</strong>
          </div>
          <div class="popup-sub">Dirección / Ref: ${est.number} (CP: ${cp})</div>
          <div class="popup-coords">Lat: ${est.latitude.toFixed(4)}, Lon: ${est.longitude.toFixed(4)}</div>
          <div class="popup-stats-box">
            <div class="stat-pill">
              <span class="pill-label">Bicis:</span>
              <span class="pill-val">${ocupadas}/${est.capacidad}</span>
            </div>
            <div class="stat-pill success">
              <span class="pill-label">Huecos:</span>
              <span class="pill-val">${libres}</span>
            </div>
          </div>
          <button id="btn-popup-ubicar-${est.id}" class="btn-popup-action">
            🚲 Ubicar bicicletas en esta estación
          </button>
        </div>
      `;

      marker.bindPopup(popupContent);
      marker.on('popupopen', () => {
        const btn = document.getElementById(`btn-popup-ubicar-${est.id}`);
        if (btn) {
          btn.onclick = () => {
            this.abrirModalUbicarBicis(est);
          };
        }
      });

      marker.addTo(this.map);
      if (est.id) {
        this.stationMarkers.set(est.id, marker);
      }
    }
  }

  centrarEstacionEnMapa(e: EstacionDto): void {
    if (e.latitude != null && e.longitude != null) {
      this.map.flyTo([e.latitude, e.longitude], 17, { duration: 1.2 });
      if (e.id && this.stationMarkers.has(e.id)) {
        setTimeout(() => {
          this.stationMarkers.get(e.id!)?.openPopup();
        }, 800);
      }
    }
  }

  // ==========================================
  // UBICAR BICICLETAS EN ESTACIÓN
  // ==========================================
  abrirModalUbicarBicis(estacion?: EstacionDto): void {
    if (estacion) {
      this.estacionParaBicis = estacion;
    } else if (this.estaciones.length > 0) {
      this.estacionParaBicis = this.estaciones[0];
    } else {
      this.mensajeError = 'No hay estaciones creadas todavía. Crea una estación primero haciendo clic en el mapa.';
      this.cdr.markForCheck();
      return;
    }

    const huecos = this.estacionParaBicis.huecosLibres || 5;
    this.cantidadBicisUbicar = Math.min(2, Math.max(1, huecos));
    this.mostrarModalUbicarBicis = true;
    this.cdr.markForCheck();
  }

  cerrarModalUbicarBicis(): void {
    this.mostrarModalUbicarBicis = false;
    this.cdr.markForCheck();
  }

  confirmarUbicarBicis(): void {
    if (!this.estacionParaBicis?.id || this.cantidadBicisUbicar < 1) return;

    this.ubicandoBicis = true;
    this.mensajeError = '';
    this.cdr.markForCheck();

    this.vehiculoService.ubicarEnEstacion(this.estacionParaBicis.id, this.cantidadBicisUbicar).subscribe({
      next: () => {
        this.ubicandoBicis = false;
        this.mostrarModalUbicarBicis = false;
        this.mensajeExito = `¡Se han ubicado exitosamente ${this.cantidadBicisUbicar} bicicleta(s) en "${this.estacionParaBicis?.name}"!`;
        this.cargarEstaciones();
        this.loadVehiculos();
        this.conectarWebSocket();
        setTimeout(() => { this.mensajeExito = ''; this.cdr.markForCheck(); }, 4500);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.ubicandoBicis = false;
        this.mensajeError = err.error?.message || 'Error al ubicar las bicicletas en la estación.';
        this.cdr.markForCheck();
      }
    });
  }

  // ==========================================
  // BICICLETAS Y MARCADORES EN MARCHA
  // ==========================================
  private crearBicycleIcon(matricula: string): L.DivIcon {
    return L.divIcon({
      className: 'bicycle-marker',
      html: `
        <div class="bicycle-marker-content">
          <div class="bicycle-icon">🚴🏼‍♀️</div>
          <div class="bicycle-label">${matricula}</div>
        </div>
      `,
      iconSize: [60, 48],
      iconAnchor: [30, 20]
    });
  }

  private actualizarMarcadoresBicis(): void {
    const matriculasActuales = new Set<string>();

    for (const vehiculo of this.vehiculos) {
      matriculasActuales.add(vehiculo.matricula);
      const marker = this.bikeMarkers.get(vehiculo.matricula);
      const lat = vehiculo.latitudActual != null ? vehiculo.latitudActual : 38.9861;
      const lon = vehiculo.longitudActual != null ? vehiculo.longitudActual : -3.9273;

      if (marker) {
        marker.setLatLng([lat, lon]);
      } else {
        const nuevoMarker = L.marker([lat, lon], {
          icon: this.crearBicycleIcon(vehiculo.matricula)
        });

        nuevoMarker.bindTooltip(`Bici #${vehiculo.matricula} (${vehiculo.bateria != null ? vehiculo.bateria : 100}% bat)`).addTo(this.map);
        this.bikeMarkers.set(vehiculo.matricula, nuevoMarker);
      }
    }

    // Eliminar bicicletas que ya no están circulando
    for (const [matricula, marker] of this.bikeMarkers) {
      if (!matriculasActuales.has(matricula)) {
        marker.remove();
        this.bikeMarkers.delete(matricula);
      }
    }
    this.cdr.markForCheck();
  }

  mostrarRuta(vehiculo: VehiculoDto): void {
    if (!this.selectedCity) return;

    this.vehiculosService.getRuta(this.selectedCity.name, vehiculo.matricula).subscribe(puntos => {
      if (this.rutaSeleccionada) {
        this.rutaSeleccionada.remove();
      }

      const latLngs: L.LatLngExpression[] = puntos.map(p => [p.latitude, p.longitude]);
      this.rutaSeleccionada = L.polyline(latLngs, {
        weight: 4,
        opacity: 0.85,
        color: '#0284c7'
      });

      this.rutaSeleccionada.addTo(this.map);
      this.cdr.markForCheck();
    });
  }

  simulacionAleatoria(): void {
    if (!this.selectedCity) return;
    this.vehiculosService.simulacionAleatoria(this.selectedCity.name).subscribe(() => {
      this.loadVehiculos();
      this.conectarWebSocket();
    });
  }

  generarBicicleta(): void {
    if (!this.selectedCity) return;
    this.vehiculosService.generateBicycle(this.selectedCity.name).subscribe(
      () => {
        this.loadVehiculos();
        this.conectarWebSocket();
      },
      err => {
        console.error(err);
      }
    );
  }

  loadVehiculos(): void {
    if (!this.selectedCity) return;
    this.vehiculosService.getVehiculos(this.selectedCity.name).subscribe(vehiculos => {
      this.vehiculos = vehiculos;
      this.actualizarMarcadoresBicis();
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    if (this.ws) {
      try { this.ws.close(); } catch {}
    }
    this.map?.remove();
  }
}