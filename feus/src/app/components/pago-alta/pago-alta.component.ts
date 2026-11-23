import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { environment } from '../../../environments/environment';

declare var Stripe: any;

@Component({
  selector: 'app-pago-alta',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="pago-container">
      <div class="card">
        <div class="icon-header">💳</div>
        <h2>Cuota de Alta Ciudadana</h2>
        <p class="subtitle">
          De conformidad con la normativa municipal (<strong>RN-CIU-02</strong>), 
          los usuarios deben abonar una fianza / cuota única de <strong>100,00 €</strong> para activar el servicio de préstamo.
        </p>

        <div class="price-box">
          <span class="price-label">Importe total a abonar:</span>
          <span class="price-value">100,00 €</span>
        </div>

        <div *ngIf="error" class="alert error">{{ error }}</div>
        <div *ngIf="mensaje" class="alert success">{{ mensaje }}</div>

        <!-- Botón para iniciar la pasarela -->
        <button 
          *ngIf="!idTransaccion && !pagoCompletado" 
          (click)="iniciarPago()" 
          [disabled]="loading" 
          class="btn-primary">
          {{ loading ? 'Conectando con Stripe...' : 'Proceder al Pago Seguro' }}
        </button>

        <!-- Formulario Stripe Card Element -->
        <div class="payment-box" [hidden]="!idTransaccion || pagoCompletado">
          <label class="stripe-label">Datos de la tarjeta de crédito o débito:</label>
          <div id="card-element" class="card-element-container"></div>
          <div id="card-errors" class="card-errors" *ngIf="cardError">{{ cardError }}</div>

          <button 
            type="button" 
            (click)="pagarConStripe()" 
            [disabled]="loadingPago" 
            class="btn-pay">
            {{ loadingPago ? 'Procesando pago de 100 €...' : 'Pagar 100,00 €' }}
          </button>
        </div>

        <div *ngIf="pagoCompletado" class="success-box">
          <h3>✅ ¡Pago confirmado con éxito!</h3>
          <p>Tu cuenta ha sido activada para el alquiler de bicicletas eléctricas.</p>
          <button (click)="irAAlquilar()" class="btn-primary">Ir al mapa y prestar bicicletas</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .pago-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 80px);
      padding: 1.5rem;
    }
    .card {
      background: white;
      padding: 2.2rem;
      border-radius: 12px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.08);
      width: 100%;
      max-width: 480px;
      text-align: center;
    }
    .icon-header {
      font-size: 2.5rem;
      margin-bottom: 0.5rem;
    }
    h2 {
      margin-bottom: 0.5rem;
      color: #0f172a;
    }
    .subtitle {
      color: #64748b;
      margin-bottom: 1.5rem;
      font-size: 0.95rem;
      line-height: 1.4;
    }
    .price-box {
      background: #f1f5f9;
      border: 1px dashed #cbd5e1;
      padding: 1.2rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .price-label {
      font-weight: 500;
      color: #334155;
    }
    .price-value {
      font-size: 1.5rem;
      font-weight: bold;
      color: #0284c7;
    }
    .payment-box {
      text-align: left;
      margin-top: 1rem;
    }
    .stripe-label {
      display: block;
      font-weight: 500;
      margin-bottom: 0.5rem;
      font-size: 0.9rem;
      color: #334155;
    }
    .card-element-container {
      padding: 0.85rem;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      background: #f8fafc;
      margin-bottom: 1rem;
    }
    .card-errors {
      color: #ef4444;
      font-size: 0.85rem;
      margin-bottom: 0.8rem;
    }
    .btn-primary, .btn-pay {
      width: 100%;
      padding: 0.85rem;
      background: #0284c7;
      color: white;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-pay {
      background: #10b981;
    }
    .btn-pay:hover:not(:disabled) {
      background: #059669;
    }
    .btn-primary:hover:not(:disabled) {
      background: #0369a1;
    }
    .btn-primary:disabled, .btn-pay:disabled {
      background: #94a3b8;
      cursor: not-allowed;
    }
    .alert {
      padding: 0.8rem;
      border-radius: 6px;
      margin-bottom: 1.2rem;
      font-size: 0.9rem;
      text-align: left;
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
    .success-box {
      margin-top: 1.5rem;
      padding: 1.2rem;
      background: #ecfdf5;
      border-radius: 8px;
    }
    .success-box h3 {
      color: #065f46;
      margin-bottom: 0.5rem;
    }
    .success-box p {
      color: #047857;
      margin-bottom: 1rem;
      font-size: 0.95rem;
    }
  `]
})
export class PagoAltaComponent implements OnInit {
  loading = false;
  loadingPago = false;
  idTransaccion: string | null = null;
  cardError: string | null = null;
  error: string | null = null;
  mensaje: string | null = null;
  pagoCompletado = false;

  private stripe: any;
  private card: any;

  constructor(private usuarioService: UsuarioService, private router: Router) {}

  ngOnInit(): void {
    if (!this.usuarioService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    // Verificar si ya tiene pagada el alta
    this.usuarioService.validateSession().subscribe({
      next: (user) => {
        if (user.pagadoAlta) {
          this.router.navigate(['/alquilar']);
        }
      }
    });

    if (typeof Stripe !== 'undefined') {
      this.stripe = Stripe(environment.stripePublicKey);
    }
  }

  iniciarPago(): void {
    this.loading = true;
    this.error = null;

    this.usuarioService.prepayAltaCiudadano().subscribe({
      next: (res) => {
        this.loading = false;
        this.idTransaccion = res.idTransaccion;
        this.montarStripeCard();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Error al inicializar la pasarela de pago';
      }
    });
  }

  private montarStripeCard(): void {
    setTimeout(() => {
      if (!this.stripe && typeof Stripe !== 'undefined') {
        this.stripe = Stripe(environment.stripePublicKey);
      }
      if (this.stripe) {
        const elements = this.stripe.elements();
        this.card = elements.create('card', {
          hidePostalCode: true
        });
        this.card.mount('#card-element');
        this.card.on('change', (event: any) => {
          this.cardError = event.error ? event.error.message : null;
        });
      }
    }, 100);
  }

  async pagarConStripe(): Promise<void> {
    if (!this.stripe || !this.card || !this.idTransaccion) {
      this.error = 'La pasarela no está lista';
      return;
    }

    this.loadingPago = true;
    this.cardError = null;
    this.error = null;

    try {
      const result = await this.stripe.confirmCardPayment(this.idTransaccion, {
        payment_method: {
          card: this.card
        }
      });

      if (result.error) {
        this.loadingPago = false;
        this.cardError = result.error.message;
        return;
      }

      if (result.paymentIntent && result.paymentIntent.status === 'succeeded') {
        // Confirmar contra backend (Paso 4.3)
        this.usuarioService.confirmarPago(this.idTransaccion).subscribe({
          next: () => {
            this.loadingPago = false;
            this.pagoCompletado = true;
            sessionStorage.setItem('user_pagadoAlta', 'true');
          },
          error: (err) => {
            this.loadingPago = false;
            this.error = 'Error al registrar la confirmación del pago en el servidor';
          }
        });
      }
    } catch (e: any) {
      this.loadingPago = false;
      this.error = e.message || 'Error inesperado durante la transacción con Stripe';
    }
  }

  irAAlquilar(): void {
    this.router.navigate(['/alquilar']);
  }
}
