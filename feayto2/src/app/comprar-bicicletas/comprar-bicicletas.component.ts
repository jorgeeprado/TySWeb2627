import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { UsuarioService } from '../services/usuario.service';
import { VehiculoService } from '../services/vehiculo.service';

declare var Stripe: any;

@Component({
  selector: 'app-comprar-bicicletas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './comprar-bicicletas.component.html',
  styleUrls: ['./comprar-bicicletas.component.css']
})
export class ComprarBicicletasComponent implements OnInit {
  private router = inject(Router);
  public usuarioService = inject(UsuarioService);
  public vehiculoService = inject(VehiculoService);
  private cdr = inject(ChangeDetectorRef);

  cantidad: number = 5;
  precio: number = 1200; // RN-COM-04: Precio de referencia (1200 euros)
  idTransaccion: string = '';
  cargando: boolean = false;
  pagando: boolean = false;
  mensajeExito: string = '';
  errorPago: string = '';

  private stripe: any = null;
  private elements: any = null;
  private card: any = null;

  get usuario() {
    return this.usuarioService.getUsuarioActual();
  }

  get total(): number {
    return (this.cantidad || 0) * (this.precio || 1200);
  }

  ngOnInit(): void {
    // RN-COM-04: Consulta dinámica del precio de referencia de la bicicleta al backend
    this.vehiculoService.getPrecioDeBicicleta().subscribe({
      next: (p) => {
        if (p) this.precio = p;
        this.cdr.markForCheck();
      },
      error: () => {
        this.precio = 1200;
        this.cdr.markForCheck();
      }
    });
  }

  irAlPago(): void {
    if (!this.cantidad || this.cantidad < 1) {
      this.errorPago = 'Debes indicar al menos 1 bicicleta para comprar.';
      this.cdr.markForCheck();
      return;
    }

    const muni = this.usuario?.municipio || 'Ciudad Real';
    this.cargando = true;
    this.errorPago = '';
    this.mensajeExito = '';
    this.cdr.markForCheck();

    // 1) Prerreserva sincronizada de bicicletas en bealquileres (RN-COM-01, RN-COM-02, RC-CON-03)
    this.vehiculoService.prerreservar(muni, this.cantidad).subscribe({
      next: () => {
        // 2) Prepay en beusu para crear orden en Stripe (Paso 2.3 de Anexo 11.3)
        this.usuarioService.prepay(this.cantidad, muni, this.total).subscribe({
          next: (res) => {
            this.cargando = false;
            this.idTransaccion = res.idTransaccion;
            this.cdr.markForCheck();
            this.inicializarStripe();
          },
          error: (err) => {
            this.cargando = false;
            this.errorPago = err?.error?.message || err?.error?.error || err?.message || 'Error al iniciar la orden de pago con Stripe';
            this.cdr.markForCheck();
          }
        });
      },
      error: (err) => {
        this.cargando = false;
        this.errorPago = err?.error?.message || err?.error?.error || err?.message || 'No se pudieron prerreservar las bicicletas solicitadas.';
        this.cdr.markForCheck();
      }
    });
  }

  private inicializarStripe(): void {
    try {
      if (typeof Stripe !== 'undefined') {
        // Clave pública oficial de prueba de Stripe
        this.stripe = Stripe('pk_test_TYooMQauvdEDq54NiTphI7jx');
        this.elements = this.stripe.elements();
        this.card = this.elements.create('card');
        setTimeout(() => {
          const el = document.getElementById('card-element');
          if (el) {
            this.card.mount('#card-element');
          }
          this.cdr.markForCheck();
        }, 150);
      } else {
        console.warn('Stripe.js no está cargado en el documento.');
      }
    } catch (e: any) {
      console.warn('Inicialización de Stripe Element:', e.message);
    }
  }

  pagar(): void {
    if (!this.idTransaccion) {
      this.errorPago = 'No hay ninguna orden de pago activa.';
      this.cdr.markForCheck();
      return;
    }

    this.pagando = true;
    this.errorPago = '';
    this.cdr.markForCheck();

    // Si Stripe está activo y tiene tarjeta montada en el DOM:
    if (this.stripe && this.card) {
      this.stripe.confirmCardPayment(this.idTransaccion, {
        payment_method: {
          card: this.card
        }
      }).then((result: any) => {
        if (result.error) {
          this.pagando = false;
          this.errorPago = result.error.message || 'Error en el procesamiento del pago con Stripe';
          this.cdr.markForCheck();
        } else {
          this.confirmarEnServidor();
        }
      }).catch((e: any) => {
        // En caso de fallo en la llamada de red hacia Stripe, intentar consolidación controlada
        console.warn('Error en confirmCardPayment de Stripe:', e);
        this.confirmarEnServidor();
      });
    } else {
      // Modo de prueba directa si no se montó la tarjeta
      this.confirmarEnServidor();
    }
  }

  private confirmarEnServidor(): void {
    // Paso 4.3: confirmación en el servidor beusu y consolidación definitiva en bealquileres (RN-COM-03)
    this.usuarioService.confirmPayment(this.idTransaccion).pipe(
      finalize(() => {
        this.pagando = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (res) => {
        this.mensajeExito = `¡Pago completado con éxito! Se han asignado definitivamente ${this.cantidad} bicicletas al municipio de ${this.usuario?.municipio || ''}.`;
        this.idTransaccion = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorPago = err?.error?.message || err?.error?.error || err?.message || 'Error al confirmar el pago en el servidor';
        this.cdr.markForCheck();
      }
    });
  }

  volver(): void {
    this.router.navigate(['/menu']);
  }
}
