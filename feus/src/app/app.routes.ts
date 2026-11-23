import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegistroComponent } from './components/registro/registro.component';
import { PagoAltaComponent } from './components/pago-alta/pago-alta.component';
import { AlquilarComponent } from './components/alquilar/alquilar.component';

export const routes: Routes = [
  { path: '', redirectTo: 'alquilar', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'pago-alta', component: PagoAltaComponent },
  { path: 'alquilar', component: AlquilarComponent },
  { path: '**', redirectTo: 'alquilar' }
];
