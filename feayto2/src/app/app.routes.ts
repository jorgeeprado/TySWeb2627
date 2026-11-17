import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegistroComponent } from './registro/registro.component';
import { MenuComponent } from './menu/menu.component';
import { ComprarBicicletasComponent } from './comprar-bicicletas/comprar-bicicletas.component';
import { UbicarEstacionComponent } from './ubicar-estacion/ubicar-estacion.component';
import { GestionarBicicletasComponent } from './gestionar-bicicletas/gestionar-bicicletas.component';
import { MapaComponent } from './mapa/mapa.component';
import { CrearVehiculosComponent } from './crear-vehiculos/crear-vehiculos.component';
import { GeoComponent } from './geo/geo.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'menu', component: MenuComponent },
  { path: 'comprar-bicicletas', component: ComprarBicicletasComponent },
  { path: 'ubicar-estacion', component: UbicarEstacionComponent },
  { path: 'gestionar-bicicletas', component: GestionarBicicletasComponent },
  { path: 'mapa', component: MapaComponent },
  { path: 'crear-vehiculos', component: CrearVehiculosComponent },
  { path: 'geo', component: GeoComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
