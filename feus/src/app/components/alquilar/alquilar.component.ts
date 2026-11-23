import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { AlquilerService } from '../../services/alquiler.service';
import { Alquiler, BicicletaDisponible, Estacion } from '../../models/alquiler.model';

declare var L: any;

@Component({
  selector: 'app-alquilar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="alquilar-container">

      <!-- BANNER DE ESTADO CIUDADANO -->
      <div class="user-status-bar">
        <div class="status-left">
          <span class="user-chip">👤 {{ userEmail }}</span>
          <span class="badge success" *ngIf="pagadoAlta">✓ Fianza 100 € abonada</span>
          <span class="badge warning" *ngIf="!pagadoAlta">⚠️ Fianza 100 € pendiente</span>
        </div>
        <div class="status-right" *ngIf="!pagadoAlta">
          <button (click)="irAPagoAlta()" class="btn-warning-small">Abonar Fianza (100 €)</button>
        </div>
      </div>

      <!-- ALERTA DE MENSAJES -->
      <div *ngIf="error" class="alert error">{{ error }}</div>
      <div *ngIf="mensaje" class="alert success">{{ mensaje }}</div>

      <!-- PANEL DE ALQUILER ACTIVO (EN CURSO) -->
      <div *ngIf="alquilerActivo" class="card active-rental-card">
        <div class="active-rental-header">
          <span class="pulse-indicator"></span>
          <h3>Alquiler en Curso</h3>
          <span class="matricula-tag">Bicicleta #{{ alquilerActivo.matricula }}</span>
        </div>

        <div class="active-rental-body">
          <div class="rental-detail-row">
            <div class="detail-item">
              <span class="label">Origen:</span>
              <span class="value">{{ alquilerActivo.estacionOrigenNombre || 'Estación Origen' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Destino:</span>
              <span class="value">{{ alquilerActivo.estacionDestinoNombre || 'Estación Destino' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Batería restante:</span>
              <span class="value battery-val" [ngClass]="getBatteryClass(bateriaActual)">
                🔋 {{ bateriaActual | number:'1.0-1' }}%
              </span>
            </div>
            <div class="detail-item">
              <span class="label">Distancia:</span>
              <span class="value">📍 {{ (alquilerActivo.distanciaRecorrida || 0) | number:'1.1-2' }} km</span>
            </div>
          </div>

          <!-- Barra de Batería en tiempo real -->
          <div class="battery-bar-container">
            <div 
              class="battery-bar-fill" 
              [style.width.%]="bateriaActual" 
              [ngClass]="getBatteryClass(bateriaActual)">
            </div>
          </div>

          <!-- MAPA DE SEGUIMIENTO EN TIEMPO REAL -->
          <div class="map-wrapper">
            <div id="map-alquiler" class="leaflet-map-view"></div>
          </div>

          <div class="actions-active">
            <button (click)="dejarBicicleta()" [disabled]="loadingAccion" class="btn-dejar">
              {{ loadingAccion ? 'Procesando devolución...' : 'Finalizar y Devolver Bicicleta (Dejar)' }}
            </button>
          </div>
        </div>
      </div>

      <!-- PANEL DE SELECCIÓN Y PRÉSTAMO (SI NO TIENE ALQUILER ACTIVO) -->
      <div *ngIf="!alquilerActivo" class="selection-grid">

        <!-- COLUMNA 1: GEOLOCALIZACIÓN Y ESTACIONES CERCANAS -->
        <div class="card col-stations">
          <h3>1. Tu Ubicación y Estaciones</h3>
          <p class="section-desc">Detectamos la estación más cercana según tu posición GPS o coordenadas de prueba (RT-LOC-03, RT-LOC-04).</p>

          <!-- Control de coordenadas manuales (RT-LOC-04) -->
          <div class="geo-controls">
            <div class="geo-inputs">
              <div class="input-inline">
                <label for="latitudManual">Latitud:</label>
                <input type="number" step="0.0001" id="latitudManual" [(ngModel)]="userLat">
              </div>
              <div class="input-inline">
                <label for="longitudManual">Longitud:</label>
                <input type="number" step="0.0001" id="longitudManual" [(ngModel)]="userLon">
              </div>
            </div>
            <div class="geo-buttons">
              <button (click)="obtenerGPS()" class="btn-secondary-small">📍 Detectar GPS</button>
              <button (click)="actualizarEstaciones()" class="btn-secondary-small">🔄 Buscar Estaciones</button>
            </div>
          </div>

          <!-- LISTA DE ESTACIONES CERCANAS -->
          <div class="station-list">
            <div 
              *ngFor="let est of estaciones" 
              (click)="seleccionarEstacion(est)" 
              class="station-item" 
              [class.selected]="estacionSeleccionada?.id === est.id">
              <div class="station-header">
                <strong>{{ est.name }}</strong>
                <span class="station-dist" *ngIf="est.distanciaMetros !== null && est.distanciaMetros !== undefined">
                  {{ formatDistancia(est.distanciaMetros) }}
                </span>
              </div>
              <div class="station-info">
                <span>Capacidad: {{ est.capacidad }}</span>
                <span class="avail-badge" [class.empty]="est.bicicletasDisponibles === 0">
                  {{ est.bicicletasDisponibles }} bicis disponibles
                </span>
              </div>
            </div>
            <div *ngIf="estaciones.length === 0" class="empty-stations">
              Cargando estaciones de Ciudad Real...
            </div>
          </div>
        </div>

        <!-- COLUMNA 2: BICICLETAS DISPONIBLES EN LA ESTACIÓN SELECCIONADA -->
        <div class="card col-bikes">
          <h3>2. Bicicletas Disponibles</h3>
          <p class="section-desc" *ngIf="estacionSeleccionada">
            Bicicletas activas en <strong>{{ estacionSeleccionada.name }}</strong> (RN-BIC-04):
          </p>
          <p class="section-desc" *ngIf="!estacionSeleccionada">
            Selecciona una estación a la izquierda para ver las bicicletas preparadas.
          </p>

          <!-- LISTADO DE BICIS DISPONIBLES CON % DE BATERÍA -->
          <div class="bikes-list" *ngIf="estacionSeleccionada">
            <div 
              *ngFor="let bici of bicisDisponibles" 
              (click)="seleccionarBici(bici)" 
              class="bike-item" 
              [class.selected]="biciSeleccionada?.id === bici.id">
              <div class="bike-top">
                <span class="bike-id">🚲 Matrícula: <strong>{{ bici.matricula }}</strong></span>
                <span class="bike-battery" [ngClass]="getBatteryClass(bici.bateria)">
                  {{ bici.bateria | number:'1.0-1' }}%
                </span>
              </div>
              <!-- Mini barra de batería -->
              <div class="battery-bar-mini">
                <div 
                  class="battery-fill-mini" 
                  [style.width.%]="bici.bateria" 
                  [ngClass]="getBatteryClass(bici.bateria)">
                </div>
              </div>
              <div class="bike-obs" *ngIf="bici.observaciones">
                Estado: {{ bici.observaciones }}
              </div>
            </div>

            <div *ngIf="bicisDisponibles.length === 0" class="empty-bikes">
              No hay bicicletas disponibles en este momento en esta estación.
            </div>
          </div>

          <!-- DESTINO Y BOTÓN DE ALQUILAR -->
          <div class="rental-action-box" *ngIf="biciSeleccionada">
            <div class="form-group">
              <label for="estacionDestinoSelect">Estación de Destino:</label>
              <select id="estacionDestinoSelect" [(ngModel)]="estacionDestinoId">
                <option [ngValue]="null">-- Automática (otra estación del municipio) --</option>
                <option *ngFor="let e of estacionesDestinoPosibles" [ngValue]="e.id">
                  {{ e.name }}
                </option>
              </select>
            </div>

            <button 
              (click)="alquilar()" 
              [disabled]="loadingAccion" 
              class="btn-alquilar">
              {{ loadingAccion ? 'Alquilando...' : '🚴 Alquilar Bicicleta #' + biciSeleccionada.matricula }}
            </button>
          </div>
        </div>

      </div>

      <!-- HISTORIAL DE ALQUILERES -->
      <div class="card history-card" *ngIf="historial.length > 0">
        <h3>Historial de Alquileres</h3>
        <div class="table-responsive">
          <table class="history-table">
            <thead>
              <tr>
                <th>Bicicleta</th>
                <th>Origen</th>
                <th>Destino</th>
                <th>Fecha Inicio</th>
                <th>Fecha Fin</th>
                <th>Distancia</th>
                <th>Batería Final</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let h of historial">
                <td>#{{ h.matricula }}</td>
                <td>{{ h.estacionOrigenNombre || '-' }}</td>
                <td>{{ h.estacionDestinoNombre || '-' }}</td>
                <td>{{ h.fechaInicio | date:'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ h.fechaFin ? (h.fechaFin | date:'dd/MM/yyyy HH:mm') : 'En curso' }}</td>
                <td>{{ (h.distanciaRecorrida || 0) | number:'1.1-2' }} km</td>
                <td>{{ h.bateria | number:'1.0-1' }}%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .alquilar-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 1.5rem;
    }
    .user-status-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: white;
      padding: 0.8rem 1.2rem;
      border-radius: 8px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.06);
      margin-bottom: 1.2rem;
    }
    .status-left {
      display: flex;
      align-items: center;
      gap: 0.8rem;
    }
    .user-chip {
      font-weight: 500;
      color: #334155;
    }
    .badge {
      padding: 0.3rem 0.6rem;
      border-radius: 4px;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .badge.success {
      background: #dcfce7;
      color: #166534;
    }
    .badge.warning {
      background: #fef3c7;
      color: #92400e;
    }
    .btn-warning-small {
      background: #f59e0b;
      color: #1e293b;
      border: none;
      padding: 0.4rem 0.8rem;
      border-radius: 6px;
      font-weight: bold;
      cursor: pointer;
    }
    .card {
      background: white;
      border-radius: 10px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.06);
      padding: 1.4rem;
      margin-bottom: 1.5rem;
    }
    .card h3 {
      color: #0f172a;
      margin-bottom: 0.3rem;
    }
    .section-desc {
      color: #64748b;
      font-size: 0.9rem;
      margin-bottom: 1rem;
    }
    .alert {
      padding: 0.8rem 1.2rem;
      border-radius: 6px;
      margin-bottom: 1.2rem;
      font-size: 0.95rem;
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
    .selection-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }
    @media (max-width: 850px) {
      .selection-grid { grid-template-columns: 1fr; }
    }
    .geo-controls {
      background: #f8fafc;
      padding: 0.9rem;
      border-radius: 6px;
      border: 1px solid #e2e8f0;
      margin-bottom: 1rem;
    }
    .geo-inputs {
      display: flex;
      gap: 1rem;
      margin-bottom: 0.6rem;
    }
    .input-inline {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .input-inline label {
      font-size: 0.8rem;
      color: #64748b;
      font-weight: 500;
    }
    .input-inline input {
      padding: 0.4rem 0.6rem;
      border: 1px solid #cbd5e1;
      border-radius: 4px;
      font-size: 0.9rem;
    }
    .geo-buttons {
      display: flex;
      gap: 0.6rem;
    }
    .btn-secondary-small {
      padding: 0.4rem 0.8rem;
      background: #e2e8f0;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      font-weight: 500;
      color: #1e293b;
    }
    .btn-secondary-small:hover {
      background: #cbd5e1;
    }
    .station-list {
      max-height: 380px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
    }
    .station-item {
      padding: 0.8rem 1rem;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .station-item:hover {
      border-color: #38bdf8;
      background: #f0f9ff;
    }
    .station-item.selected {
      border-color: #0284c7;
      background: #e0f2fe;
    }
    .station-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.3rem;
      color: #1e293b;
    }
    .station-dist {
      font-size: 0.85rem;
      color: #0284c7;
      font-weight: 600;
    }
    .station-info {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
      color: #64748b;
    }
    .avail-badge {
      font-weight: 600;
      color: #166534;
    }
    .avail-badge.empty {
      color: #dc2626;
    }
    .bikes-list {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.8rem;
      max-height: 320px;
      overflow-y: auto;
      margin-bottom: 1.2rem;
    }
    @media (max-width: 500px) {
      .bikes-list { grid-template-columns: 1fr; }
    }
    .bike-item {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 0.8rem;
      cursor: pointer;
      background: #fafafa;
      transition: all 0.2s;
    }
    .bike-item:hover {
      border-color: #0284c7;
      background: #f0fdf4;
    }
    .bike-item.selected {
      border-color: #10b981;
      background: #ecfdf5;
      box-shadow: 0 0 0 2px #10b981;
    }
    .bike-top {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.4rem;
    }
    .bike-id {
      font-size: 0.95rem;
      color: #1e293b;
    }
    .battery-bar-mini {
      height: 6px;
      background: #e2e8f0;
      border-radius: 3px;
      overflow: hidden;
      margin-bottom: 0.4rem;
    }
    .battery-fill-mini {
      height: 100%;
      border-radius: 3px;
      transition: width 0.3s;
    }
    .battery-high { background-color: #10b981; color: #047857; }
    .battery-med { background-color: #f59e0b; color: #b45309; }
    .battery-low { background-color: #ef4444; color: #b91c1c; }
    .bike-obs {
      font-size: 0.75rem;
      color: #64748b;
    }
    .rental-action-box {
      border-top: 1px solid #e2e8f0;
      padding-top: 1rem;
    }
    .form-group {
      margin-bottom: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }
    select {
      padding: 0.6rem;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      font-size: 0.95rem;
    }
    .btn-alquilar {
      width: 100%;
      padding: 0.85rem;
      background: #10b981;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 1.05rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-alquilar:hover:not(:disabled) {
      background: #059669;
    }
    .btn-alquilar:disabled {
      background: #94a3b8;
      cursor: not-allowed;
    }
    /* ACTIVE RENTAL CARD */
    .active-rental-card {
      border: 2px solid #38bdf8;
      background: #f0f9ff;
    }
    .active-rental-header {
      display: flex;
      align-items: center;
      gap: 0.8rem;
      margin-bottom: 1rem;
    }
    .pulse-indicator {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: #10b981;
      box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
      animation: pulse 1.6s infinite;
    }
    @keyframes pulse {
      0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
      70% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
      100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }
    .matricula-tag {
      background: #0284c7;
      color: white;
      padding: 0.2rem 0.6rem;
      border-radius: 4px;
      font-weight: 600;
      font-size: 0.9rem;
    }
    .rental-detail-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
      background: white;
      padding: 1rem;
      border-radius: 8px;
      margin-bottom: 1rem;
    }
    .detail-item {
      display: flex;
      flex-direction: column;
    }
    .detail-item .label {
      font-size: 0.8rem;
      color: #64748b;
    }
    .detail-item .value {
      font-size: 1.05rem;
      font-weight: 600;
      color: #0f172a;
    }
    .battery-bar-container {
      height: 10px;
      background: #e2e8f0;
      border-radius: 5px;
      overflow: hidden;
      margin-bottom: 1rem;
    }
    .battery-bar-fill {
      height: 100%;
      transition: width 0.5s ease;
    }
    .map-wrapper {
      margin-bottom: 1.2rem;
    }
    .leaflet-map-view {
      height: 320px;
      width: 100%;
      border-radius: 8px;
      border: 1px solid #cbd5e1;
    }
    .btn-dejar {
      width: 100%;
      padding: 0.85rem;
      background: #ef4444;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
    }
    .btn-dejar:hover:not(:disabled) {
      background: #dc2626;
    }
    .btn-dejar:disabled {
      background: #94a3b8;
    }
    .history-card {
      margin-top: 2rem;
    }
    .table-responsive {
      overflow-x: auto;
    }
    .history-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }
    .history-table th, .history-table td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #e2e8f0;
    }
    .history-table th {
      background: #f8fafc;
      color: #475569;
      font-weight: 600;
    }
  `]
})
export class AlquilarComponent implements OnInit, OnDestroy, AfterViewInit {
  userEmail = '';
  pagadoAlta = false;

  // Ubicación del usuario (RT-LOC-03, RT-LOC-04)
  userLat = 38.9861;
  userLon = -3.9273;

  estaciones: Estacion[] = [];
  estacionSeleccionada: Estacion | null = null;
  estacionDestinoId: number | null = null;
  bicisDisponibles: BicicletaDisponible[] = [];
  biciSeleccionada: BicicletaDisponible | null = null;

  alquilerActivo: Alquiler | null = null;
  bateriaActual = 100.0;
  historial: Alquiler[] = [];

  loadingAccion = false;
  error: string | null = null;
  mensaje: string | null = null;

  private map: any = null;
  private bikeMarker: any = null;
  private timerPolling: any = null;

  constructor(
    private usuarioService: UsuarioService,
    private alquilerService: AlquilerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.usuarioService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.userEmail = sessionStorage.getItem('user_email') || 'ciudadano';
    this.pagadoAlta = this.usuarioService.hasPagadoAlta();

    // Validar sesión contra backend
    this.usuarioService.validateSession().subscribe({
      next: (u) => {
        this.pagadoAlta = !!u.pagadoAlta;
        if (!this.pagadoAlta) {
          this.mensaje = 'Recuerda que para alquilar necesitas abonar la cuota de alta de 100 € (RN-CIU-02).';
        }
      }
    });

    // Inicializar ubicación GPS
    this.obtenerGPS();

    // Consultar estado de alquiler
    this.consultarAlquilerActivo();
    this.cargarHistorial();

    // Polling cada 4 segundos para actualizar estado en simulador
    this.timerPolling = setInterval(() => {
      if (this.alquilerActivo) {
        this.consultarAlquilerActivo();
      }
    }, 4000);
  }

  ngAfterViewInit(): void {
    if (this.alquilerActivo) {
      setTimeout(() => this.inicializarMapa(), 200);
    }
  }

  ngOnDestroy(): void {
    if (this.timerPolling) {
      clearInterval(this.timerPolling);
    }
    if (this.map) {
      this.map.remove();
    }
  }

  get estacionesDestinoPosibles(): Estacion[] {
    if (!this.estacionSeleccionada) return this.estaciones;
    return this.estaciones.filter(e => e.id !== this.estacionSeleccionada?.id);
  }

  obtenerGPS(): void {
    if (typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          this.userLat = Math.round(pos.coords.latitude * 10000) / 10000;
          this.userLon = Math.round(pos.coords.longitude * 10000) / 10000;
          this.actualizarEstaciones();
        },
        () => {
          this.actualizarEstaciones();
        },
        { timeout: 4000 }
      );
    } else {
      this.actualizarEstaciones();
    }
  }

  actualizarEstaciones(): void {
    this.alquilerService.getEstaciones('Ciudad Real', this.userLat, this.userLon).subscribe({
      next: (data) => {
        this.estaciones = data;
        if (data.length > 0 && !this.estacionSeleccionada) {
          this.seleccionarEstacion(data[0]);
        }
      },
      error: () => {
        this.error = 'No se pudieron recuperar las estaciones del municipio';
      }
    });
  }

  seleccionarEstacion(est: Estacion): void {
    this.estacionSeleccionada = est;
    this.biciSeleccionada = null;
    this.alquilerService.getBicisDisponiblesEnEstacion(est.id).subscribe({
      next: (bicis) => {
        this.bicisDisponibles = bicis;
        if (bicis.length > 0) {
          this.seleccionarBici(bicis[0]);
        }
      }
    });
  }

  seleccionarBici(bici: BicicletaDisponible): void {
    this.biciSeleccionada = bici;
  }

  alquilar(): void {
    if (!this.biciSeleccionada) return;

    if (!this.pagadoAlta) {
      this.error = 'Debes abonar la cuota de alta de 100 € antes de alquilar bicicletas (RN-CIU-02)';
      return;
    }

    this.loadingAccion = true;
    this.error = null;

    this.alquilerService.alquilar(this.biciSeleccionada.id, this.estacionDestinoId || undefined).subscribe({
      next: (res) => {
        this.loadingAccion = false;
        this.alquilerActivo = res;
        this.bateriaActual = res.bateria || 100.0;
        this.mensaje = `¡Bicicleta #${res.matricula} alquilada correctamente! El simulador ha iniciado el recorrido.`;
        setTimeout(() => this.inicializarMapa(), 200);
      },
      error: (err) => {
        this.loadingAccion = false;
        this.error = err.error?.message || 'Error al procesar el alquiler';
      }
    });
  }

  consultarAlquilerActivo(): void {
    this.alquilerService.getAlquilerActivo().subscribe({
      next: (alquiler) => {
        const eraActivo = !!this.alquilerActivo;
        this.alquilerActivo = alquiler;

        if (alquiler) {
          this.bateriaActual = alquiler.bateria || this.bateriaActual;
          this.actualizarPosicionSimulador(alquiler.matricula);

          if (!eraActivo) {
            setTimeout(() => this.inicializarMapa(), 200);
          }
        } else if (eraActivo && !alquiler) {
          // El simulador ha completado la devolución automática (RN-ALQ-03)
          this.mensaje = 'La bicicleta ha llegado a su estación de destino y el alquiler ha finalizado automáticamente (RN-ALQ-03).';
          this.actualizarEstaciones();
          this.cargarHistorial();
        }
      }
    });
  }

  actualizarPosicionSimulador(matricula: string): void {
    this.alquilerService.getVehiculosEnCirculacion('Ciudad Real').subscribe({
      next: (vehiculos) => {
        const v = vehiculos.find(item => item.matricula === matricula || item.matricula === String(matricula).padStart(4, '0'));
        if (v) {
          if (v.bateria !== undefined) {
            this.bateriaActual = v.bateria;
          }
          if (this.alquilerActivo && v.distanciaRecorrida !== undefined) {
            this.alquilerActivo.distanciaRecorrida = v.distanciaRecorrida / 1000.0;
          }
          if (this.map && this.bikeMarker && v.latitudActual && v.longitudActual) {
            this.bikeMarker.setLatLng([v.latitudActual, v.longitudActual]);
            this.bikeMarker.setPopupContent(`<b>Bici #${matricula}</b><br>Batería: ${Math.round(this.bateriaActual)}%<br>Velocidad: ${Math.round(v.kmh || 0)} km/h`);
          }
        }
      }
    });
  }

  dejarBicicleta(): void {
    if (!this.alquilerActivo) return;

    this.loadingAccion = true;
    this.error = null;

    this.alquilerService.dejar(this.alquilerActivo.vehiculoId, this.alquilerActivo.estacionDestinoId).subscribe({
      next: () => {
        this.loadingAccion = false;
        this.alquilerActivo = null;
        this.mensaje = 'Bicicleta devuelta con éxito.';
        this.actualizarEstaciones();
        this.cargarHistorial();
      },
      error: (err) => {
        this.loadingAccion = false;
        this.error = err.error?.message || 'Error al devolver la bicicleta';
      }
    });
  }

  cargarHistorial(): void {
    this.alquilerService.getHistorial().subscribe({
      next: (h) => this.historial = h
    });
  }

  inicializarMapa(): void {
    if (typeof L === 'undefined' || !document.getElementById('map-alquiler')) return;

    if (this.map) {
      this.map.remove();
      this.map = null;
    }

    const lat = this.userLat || 38.9861;
    const lon = this.userLon || -3.9273;

    this.map = L.map('map-alquiler').setView([lat, lon], 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.bikeMarker = L.marker([lat, lon]).addTo(this.map)
      .bindPopup(`<b>Bicicleta en tránsito</b><br>Batería: ${Math.round(this.bateriaActual)}%`)
      .openPopup();
  }

  getBatteryClass(bateria: number): string {
    if (bateria >= 50) return 'battery-high';
    if (bateria >= 20) return 'battery-med';
    return 'battery-low';
  }

  formatDistancia(metros: number): string {
    if (metros < 1000) return `${Math.round(metros)} m`;
    return `${(metros / 1000).toFixed(1)} km`;
  }

  irAPagoAlta(): void {
    this.router.navigate(['/pago-alta']);
  }
}
