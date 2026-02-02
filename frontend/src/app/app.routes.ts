import { Routes } from '@angular/router';
import { AuthGuard } from './services/auth.guard';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./modulos/login/login').then(m => m.LoginComponent)
  },
  // ✅ AQUÍ AGREGAMOS LA RUTA DE REGISTRO
  {
    path: 'registro',
    loadComponent: () => import('./modulos/Registro/registro').then(m => m.RegistroComponent)
  },
  {
    path: 'postulante',
    loadComponent: () => import('./modulos/Postulante/postulante').then(m => m.PostulanteComponent),
    canActivate: [AuthGuard],
    data: { rol: 'postulante' }
  },
  {
    path: 'evaluador',
    loadComponent: () => import('./modulos/Evaluador/evaluador').then(m => m.EvaluadorComponent),
    canActivate: [AuthGuard],
    data: { rol: 'evaluador' }
  },
  {
    path: 'admin',
    loadComponent: () => import('./modulos/Admin/admin').then(m => m.AdminComponent),
    canActivate: [AuthGuard],
    data: { rol: 'admin' }
  },
  {
    path: 'crear-evaluador',
    loadComponent: () => import('./modulos/crear-auto/crear-auto')
      .then(m => m.CrearEvaluadorComponent)
  }
  ,

  {
    path: '**',
    redirectTo: 'login'
  }

];
