import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'socios', loadComponent: () => import('./pages/socios/socios').then((m) => m.Socios) },
      { path: 'giros', loadComponent: () => import('./pages/giros/giros').then((m) => m.Giros) },
      { path: 'puestos', loadComponent: () => import('./pages/puestos/puestos').then((m) => m.Puestos) },
      { path: 'servicios', loadComponent: () => import('./pages/servicios/servicios').then((m) => m.Servicios) },
      { path: 'cuentas', loadComponent: () => import('./pages/cuentas/cuentas').then((m) => m.Cuentas) },
      { path: 'resumen', loadComponent: () => import('./pages/resumen/resumen').then((m) => m.Resumen) },
      { path: 'recibos', loadComponent: () => import('./pages/recibos/recibos').then((m) => m.Recibos) },
      { path: 'bancos', loadComponent: () => import('./pages/bancos/bancos').then((m) => m.Bancos) },
      { path: 'operaciones', loadComponent: () => import('./pages/operaciones/operaciones').then((m) => m.Operaciones) },
      { path: 'egresos', loadComponent: () => import('./pages/egresos/egresos').then((m) => m.Egresos) },
      { path: 'reportes', loadComponent: () => import('./pages/reportes/reportes').then((m) => m.Reportes) },
    ],
  },
  { path: '**', redirectTo: '' },
];
